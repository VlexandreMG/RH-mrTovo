package com.mrtovo.backend.bloc3.service;

import com.mrtovo.backend.bloc3.domain.Acteur;
import com.mrtovo.backend.bloc3.domain.HistoriqueNegociation;
import com.mrtovo.backend.bloc3.domain.Negociation;
import com.mrtovo.backend.bloc3.domain.PropositionOffre;
import com.mrtovo.backend.bloc3.domain.StatutCandidature;
import com.mrtovo.backend.bloc3.domain.StatutNegociation;
import com.mrtovo.backend.bloc3.domain.StatutProposition;
import com.mrtovo.backend.bloc3.domain.TypeNegociation;
import com.mrtovo.backend.bloc3.exception.ApiException;
import com.mrtovo.backend.bloc3.repository.HistoriqueNegociationRepository;
import com.mrtovo.backend.bloc3.repository.NegociationRepository;
import com.mrtovo.backend.bloc3.web.dto.ActionRequest;
import com.mrtovo.backend.bloc3.web.dto.DecisionNegociationRequest;
import com.mrtovo.backend.bloc3.web.dto.GrilleDto;
import com.mrtovo.backend.bloc3.web.dto.NegociationDto;
import com.mrtovo.backend.bloc3.web.dto.PropositionDto;
import com.mrtovo.backend.bloc3.web.dto.RevendicationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NegociationService {

    private final NegociationRepository negociationRepository;
    private final HistoriqueNegociationRepository historiqueRepository;
    private final PropositionService propositionService;
    private final GrilleSalarialeService grilleSalarialeService;
    private final Bloc3Mapper mapper;

    public NegociationService(NegociationRepository negociationRepository,
                              HistoriqueNegociationRepository historiqueRepository,
                              PropositionService propositionService,
                              GrilleSalarialeService grilleSalarialeService,
                              Bloc3Mapper mapper) {
        this.negociationRepository = negociationRepository;
        this.historiqueRepository = historiqueRepository;
        this.propositionService = propositionService;
        this.grilleSalarialeService = grilleSalarialeService;
        this.mapper = mapper;
    }

    public List<NegociationDto> lister() {
        return negociationRepository.findAllByOrderByDateDemandeDesc().stream().map(mapper::toNegociationDto).toList();
    }

    public NegociationDto detail(Long id) {
        return mapper.toNegociationDto(charger(id));
    }

    public NegociationDto deposerRevendication(Long propositionId, RevendicationRequest request) {
        PropositionOffre proposition = propositionService.chargerPourMaj(propositionId);
        if (proposition.getStatut() != StatutProposition.EMISE) {
            throw ApiException.conflit("PROPOSITION_NON_EMISE",
                    "Seule une proposition EMISE peut faire l'objet d'une revendication (statut actuel : "
                            + proposition.getStatut() + ")");
        }
        negociationsOuvertes(proposition).stream().findFirst().ifPresent(negociation -> {
            throw ApiException.conflit("NEGOCIATION_EN_COURS",
                    "Une negociation est deja en cours sur la proposition " + propositionId);
        });

        propositionService.transitionner(proposition, StatutProposition.EN_NEGOCIATION, "REVOCATION_CANDIDAT",
                Acteur.CANDIDAT.name(), request.motif());
        propositionService.changerStatutCandidature(proposition.getCandidature(), StatutCandidature.EN_NEGOCIATION);

        Negociation negociation = new Negociation();
        negociation.setProposition(proposition);
        negociation.setCandidat(proposition.getCandidature().getCandidat());
        negociation.setType(request.type() != null ? request.type() : TypeNegociation.REVENDICATION);
        negociation.setSalaireRevendique(request.salaireRevendique());
        negociation.setAvantageRevendique(request.avantageRevendique());
        negociation.setMotif(request.motif());
        negociation.setDateDemande(LocalDate.now());
        negociation.setStatut(StatutNegociation.EN_ATTENTE);
        negociation = negociationRepository.save(negociation);

        historiser(negociation, null, StatutNegociation.EN_ATTENTE, "REVENDICATION",
                Acteur.CANDIDAT.name(), request.motif());
        return mapper.toNegociationDto(negociation);
    }

    public PropositionDto annulerProposition(Long propositionId, ActionRequest request) {
        PropositionOffre proposition = propositionService.chargerPourMaj(propositionId);
        String auteur = request.acteurOuDefaut(Acteur.RH);
        for (Negociation ouverte : negociationsOuvertes(proposition)) {
            transitionner(ouverte, StatutNegociation.REFUSE, "CLOTURE_ANNULATION", auteur,
                    "Proposition annulee");
            ouverte.setDecidePar(auteur);
            ouverte.setDateDecision(LocalDate.now());
            negociationRepository.save(ouverte);
        }
        return propositionService.annuler(propositionId, request);
    }

    public NegociationDto analyser(Long id, ActionRequest request) {
        Negociation negociation = charger(id);
        transitionner(negociation, StatutNegociation.EN_ANALYSE, "ANALYSE",
                request.acteurOuDefaut(Acteur.RH), request.commentaire());
        return mapper.toNegociationDto(negociation);
    }

    public NegociationDto decider(Long id, DecisionNegociationRequest request) {
        Negociation negociation = charger(id);
        String decision = normaliserDecision(request.decision());
        PropositionOffre proposition = propositionService.chargerPourMaj(negociation.getProposition().getId());
        String auteur = request.acteurOuDefaut(Acteur.RH);

        if (decision.equals("REFUSE")) {
            transitionner(negociation, StatutNegociation.REFUSE, "DECISION_REFUSEE", auteur, request.commentaire());
            negociation.setDecidePar(auteur);
            negociation.setDateDecision(LocalDate.now());
            negociation.setCommentaireDecision(request.commentaire());
            negociationRepository.save(negociation);

            if (proposition.getStatut() == StatutProposition.EN_NEGOCIATION) {
                propositionService.transitionner(proposition, StatutProposition.REFUSEE, "REFUS_NEGOCIATION", auteur,
                        request.commentaire());
                propositionService.changerStatutCandidature(proposition.getCandidature(), StatutCandidature.REFUSE);
            }
            return mapper.toNegociationDto(negociation);
        }

        BigDecimal salaireAccorde = request.salaireAccorde() != null
                ? request.salaireAccorde()
                : negociation.getSalaireRevendique();
        if (salaireAccorde == null) {
            // Revendication portant uniquement sur un avantage : le salaire courant est conserve
            salaireAccorde = proposition.getSalaireBase();
        }
        controlerGrille(proposition, salaireAccorde, request.derogationAccordee(), request.motifDerogation());

        String motif = (request.commentaire() == null ? "" : request.commentaire())
                + (decision.equals("CONTRE_PROPOSITION") ? " | Contre-proposition de l'entreprise" : " | Revendication acceptee");
        // Ordre important : l'ancienne version doit etre SUPERSEDEE (et flushee) avant l'insertion de la
        // nouvelle, pour respecter l'index unique "une seule proposition active par candidature".
        propositionService.transitionner(proposition, StatutProposition.SUPERSEDEE, "REMPLACEE_PAR_VERSION",
                auteur, "Remplacee suite a la negociation : " + motif);
        PropositionOffre revision = propositionService.creerVersionRevisee(proposition, salaireAccorde,
                request.avantages(), motif, auteur);

        transitionner(negociation,
                decision.equals("CONTRE_PROPOSITION") ? StatutNegociation.CONTRE_PROPOSITION : StatutNegociation.ACCEPTE,
                decision.equals("CONTRE_PROPOSITION") ? "CONTRE_PROPOSITION" : "DECISION_ACCEPTEE",
                auteur, request.commentaire());
        negociation.setDecidePar(auteur);
        finaliserDecision(negociation, request, revision);
        return mapper.toNegociationDto(negociation);
    }

    private void finaliserDecision(Negociation negociation, DecisionNegociationRequest request,
                                   PropositionOffre revision) {
        negociation.setDateDecision(LocalDate.now());
        negociation.setCommentaireDecision(request.commentaire());
        negociation.setPropositionResultante(revision);
        negociationRepository.save(negociation);
    }

    private void controlerGrille(PropositionOffre proposition, BigDecimal salaireAccorde, boolean derogation,
                                 String motifDerogation) {
        Long posteId = proposition.getPoste() != null ? proposition.getPoste().getId() : null;
        if (!grilleSalarialeService.postePossedeUneGrille(posteId)) {
            return;
        }
        GrilleDto grille = grilleSalarialeService.niveauPourSalaire(posteId, salaireAccorde, LocalDate.now());
        if (grille != null) {
            return;
        }
        if (derogation) {
            if (motifDerogation == null || motifDerogation.isBlank()) {
                throw ApiException.badRequest("MOTIF_DEROGATION_MANQUANT",
                        "Le motif de la derogation est obligatoire");
            }
            return;
        }
        StringBuilder message = new StringBuilder("SALAIRE_HORS_GRILLE : le salaire ")
                .append(salaireAccorde)
                .append(" ne correspond a aucun niveau de la grille du poste ");
        if (proposition.getPoste() != null) {
            message.append('"').append(proposition.getPoste().getTitre()).append('"');
        }
        message.append(". Une derogation RH avec motif est requise pour valider cette negociation.");
        throw ApiException.conflit("SALAIRE_HORS_GRILLE", message.toString());
    }

    private String normaliserDecision(String decision) {
        if (decision == null || decision.isBlank()) {
            throw ApiException.badRequest("DECISION_MANQUANTE", "La decision est obligatoire");
        }
        String valeur = decision.trim().toUpperCase().replace(" ", "_");
        return switch (valeur) {
            case "ACCEPTE", "ACCEPTEE", "ACCEPTER", "ACCORD_PARTIEL" -> "ACCEPTE";
            case "REFUSE", "REFUSEE", "REFUSER" -> "REFUSE";
            case "CONTRE_PROPOSITION", "CONTREPROPOSITION" -> "CONTRE_PROPOSITION";
            default -> throw ApiException.badRequest("DECISION_INCONNUE",
                    "Decision inconnue : " + decision + " (ACCEPTE, REFUSE ou CONTRE_PROPOSITION)");
        };
    }

    private List<Negociation> negociationsOuvertes(PropositionOffre proposition) {
        return negociationRepository.findByPropositionIdOrderByDateDemandeDesc(proposition.getId()).stream()
                .filter(negociation -> negociation.getStatut() == StatutNegociation.EN_ATTENTE
                        || negociation.getStatut() == StatutNegociation.EN_ANALYSE
                        || negociation.getStatut() == StatutNegociation.CONTRE_PROPOSITION)
                .toList();
    }

    private Negociation charger(Long id) {
        return negociationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Negociation introuvable : " + id));
    }

    private void transitionner(Negociation negociation, StatutNegociation cible, String action, String auteur,
                               String commentaire) {
        StatutNegociation actuel = negociation.getStatut();
        if (!actuel.peutEvoluerVers(cible)) {
            throw ApiException.conflit("TRANSITION_INVALIDE",
                    "Transition impossible de " + actuel + " vers " + cible + " (transitions autorisees : "
                            + actuel.transitionsAutorisees() + ")");
        }
        negociation.setStatut(cible);
        negociationRepository.save(negociation);
        historiser(negociation, actuel, cible, action, auteur, commentaire);
    }

    private void historiser(Negociation negociation, StatutNegociation ancien, StatutNegociation nouveau,
                            String action, String auteur, String commentaire) {
        HistoriqueNegociation historique = new HistoriqueNegociation();
        historique.setNegociation(negociation);
        historique.setAncienStatut(ancien != null ? ancien.name() : null);
        historique.setNouveauStatut(nouveau.name());
        historique.setAction(action);
        historique.setAuteur(auteur);
        historique.setCommentaire(commentaire);
        historique.setDateChangement(LocalDateTime.now());
        historiqueRepository.save(historique);
    }
}
