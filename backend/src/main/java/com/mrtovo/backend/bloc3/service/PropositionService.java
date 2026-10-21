package com.mrtovo.backend.bloc3.service;

import com.mrtovo.backend.bloc3.config.Bloc3Properties;
import com.mrtovo.backend.bloc3.domain.Acteur;
import com.mrtovo.backend.bloc3.domain.AvantageOffre;
import com.mrtovo.backend.bloc3.domain.Candidature;
import com.mrtovo.backend.bloc3.domain.Departement;
import com.mrtovo.backend.bloc3.domain.HistoriqueProposition;
import com.mrtovo.backend.bloc3.domain.Poste;
import com.mrtovo.backend.bloc3.domain.PropositionOffre;
import com.mrtovo.backend.bloc3.domain.StatutCandidature;
import com.mrtovo.backend.bloc3.domain.StatutProposition;
import com.mrtovo.backend.bloc3.domain.TypeAvantage;
import com.mrtovo.backend.bloc3.domain.TypeContrat;
import com.mrtovo.backend.bloc3.exception.ApiException;
import com.mrtovo.backend.bloc3.repository.AvantageOffreRepository;
import com.mrtovo.backend.bloc3.repository.CandidatureRepository;
import com.mrtovo.backend.bloc3.repository.DepartementRepository;
import com.mrtovo.backend.bloc3.repository.HistoriquePropositionRepository;
import com.mrtovo.backend.bloc3.repository.PosteRepository;
import com.mrtovo.backend.bloc3.repository.PropositionOffreRepository;
import com.mrtovo.backend.bloc3.web.dto.AcceptionRequest;
import com.mrtovo.backend.bloc3.web.dto.ActionRequest;
import com.mrtovo.backend.bloc3.web.dto.AvantageRequest;
import com.mrtovo.backend.bloc3.web.dto.EmissionRequest;
import com.mrtovo.backend.bloc3.web.dto.GrilleDto;
import com.mrtovo.backend.bloc3.web.dto.PropositionCreateRequest;
import com.mrtovo.backend.bloc3.web.dto.PropositionDto;
import com.mrtovo.backend.bloc3.web.dto.ValidationRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PropositionService {

    private final PropositionOffreRepository propositionRepository;
    private final CandidatureRepository candidatureRepository;
    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final AvantageOffreRepository avantageRepository;
    private final HistoriquePropositionRepository historiqueRepository;
    private final GrilleSalarialeService grilleSalarialeService;
    private final EmployeService employeService;
    private final Bloc3Mapper mapper;
    private final Bloc3Properties properties;

    public PropositionService(PropositionOffreRepository propositionRepository,
                              CandidatureRepository candidatureRepository,
                              PosteRepository posteRepository,
                              DepartementRepository departementRepository,
                              AvantageOffreRepository avantageRepository,
                              HistoriquePropositionRepository historiqueRepository,
                              GrilleSalarialeService grilleSalarialeService,
                              EmployeService employeService,
                              Bloc3Mapper mapper,
                              Bloc3Properties properties) {
        this.propositionRepository = propositionRepository;
        this.candidatureRepository = candidatureRepository;
        this.posteRepository = posteRepository;
        this.departementRepository = departementRepository;
        this.avantageRepository = avantageRepository;
        this.historiqueRepository = historiqueRepository;
        this.grilleSalarialeService = grilleSalarialeService;
        this.employeService = employeService;
        this.mapper = mapper;
        this.properties = properties;
    }

    public List<PropositionDto> lister(String statut, Long candidatureId) {
        List<PropositionOffre> propositions;
        if (candidatureId != null) {
            propositions = propositionRepository.findByCandidatureIdOrderByVersionAsc(candidatureId);
        } else if (statut != null && !statut.isBlank()) {
            propositions = propositionRepository.findByStatutOrderByDateEmissionDesc(
                    StatutProposition.valueOf(statut.trim().toUpperCase()));
        } else {
            propositions = propositionRepository.findAllByOrderByDateEmissionDesc();
        }
        return mapper.toPropositionDtos(propositions);
    }

    public PropositionDto detail(Long id) {
        return mapper.toPropositionDto(charger(id));
    }

    public PropositionDto creer(PropositionCreateRequest request) {
        Candidature candidature = candidatureRepository.findById(request.candidatureId())
                .orElseThrow(() -> ApiException.notFound("Candidature introuvable : " + request.candidatureId()));

        if (candidature.getStatut() != StatutCandidature.RETENU) {
            throw ApiException.conflit("CANDIDATURE_NON_ELIGIBLE",
                    "La candidature " + candidature.getId() + " doit etre au statut RETENU (statut actuel : "
                            + candidature.getStatut() + ")");
        }
        boolean propositionEnCours = propositionRepository.findByCandidatureIdOrderByVersionAsc(candidature.getId())
                .stream().anyMatch(proposition -> !proposition.getStatut().estTerminal());
        if (propositionEnCours) {
            throw ApiException.conflit("PROPOSITION_EXISTANTE",
                    "Une proposition est deja en cours pour la candidature " + candidature.getId());
        }
        if (request.posteId() == null) {
            throw ApiException.badRequest("POSTE_MANQUANT", "Le poste cible est obligatoire");
        }

        Poste poste = posteRepository.findById(request.posteId())
                .orElseThrow(() -> ApiException.notFound("Poste introuvable : " + request.posteId()));
        Departement departement = request.departementId() != null
                ? departementRepository.findById(request.departementId())
                        .orElseThrow(() -> ApiException.notFound("Departement introuvable : " + request.departementId()))
                : poste.getDepartement();

        TypeContrat typeContrat = request.typeContrat() != null ? request.typeContrat() : TypeContrat.CDI;
        int periodeEssai = request.periodeEssaiMois() != null
                ? request.periodeEssaiMois()
                : periodeEssaiParDefaut(typeContrat);
        LocalDate aujourdhui = LocalDate.now();
        LocalDate dateLimite = request.dateLimiteValidite() != null
                ? request.dateLimiteValidite()
                : aujourdhui.plusDays(properties.getDureeValiditeJours());
        if (dateLimite.isBefore(aujourdhui)) {
            throw ApiException.badRequest("DATE_LIMITE_INVALIDE",
                    "La date limite de validite ne peut pas etre anterieure a aujourd'hui");
        }
        controlerDureeCdd(typeContrat, request.datePrisePoste(), request.dateFinContrat());

        PropositionOffre proposition = new PropositionOffre();
        proposition.setCandidature(candidature);
        proposition.setVersion(propositionRepository.maxVersionPourCandidature(candidature.getId()) + 1);
        proposition.setSalaireBase(request.salaireBase());
        proposition.setTempsTravail(request.tempsTravail());
        proposition.setEmploiDuTemps(request.emploiDuTemps());
        proposition.setLieuTravail(request.lieuTravail());
        proposition.setDatePrisePoste(request.datePrisePoste());
        proposition.setDateLimiteValidite(dateLimite);
        proposition.setDateEmission(aujourdhui);
        proposition.setDateFinContrat(request.dateFinContrat());
        proposition.setTypeContrat(typeContrat);
        proposition.setPeriodeEssaiMois(periodeEssai);
        proposition.setPoste(poste);
        proposition.setDepartement(departement);
        proposition.setStatut(StatutProposition.BROUILLON);
        proposition.setMotifRevision(request.motif());
        proposition = propositionRepository.save(proposition);

        ajouterAvantages(proposition, request.avantages());
        historiser(proposition, null, StatutProposition.BROUILLON, "CREATION", Acteur.RH.name(), request.motif());
        return mapper.toPropositionDto(proposition);
    }

    public PropositionDto valider(Long id, ValidationRequest request) {
        PropositionOffre proposition = charger(id);
        boolean horsGrille = controlerGrilleSalariale(proposition, request.derogationAccordee());
        String commentaire = request.commentaire();
        if (horsGrille) {
            commentaire = (commentaire == null ? "" : commentaire + " | ")
                    + "Derogation hors grille : " + request.motifDerogation();
        }
        transitionner(proposition, StatutProposition.VALIDEE, "VALIDATION_RH",
                request.acteurOuDefaut(Acteur.RH), commentaire);
        proposition.setValideePar(request.acteurOuDefaut(Acteur.RH));
        proposition.setDateValidation(LocalDate.now());
        propositionRepository.save(proposition);
        return mapper.toPropositionDto(proposition);
    }

    public PropositionDto emettre(Long id, EmissionRequest request) {
        PropositionOffre proposition = charger(id);
        if (request.dateLimiteValidite() != null) {
            proposition.setDateLimiteValidite(request.dateLimiteValidite());
        }
        if (proposition.getDateLimiteValidite().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("DATE_LIMITE_INVALIDE",
                    "La date limite de validite ne peut pas etre anterieure a aujourd'hui");
        }
        transitionner(proposition, StatutProposition.EMISE, "EMISSION",
                request.acteurOuDefaut(Acteur.RECRUTEUR), request.commentaire());
        proposition.setDateEmission(LocalDate.now());
        propositionRepository.save(proposition);
        changerStatutCandidature(proposition.getCandidature(), StatutCandidature.PROPOSITION_ENVOYEE);
        return mapper.toPropositionDto(proposition);
    }

    public PropositionDto accepter(Long id, AcceptionRequest request) {
        PropositionOffre proposition = charger(id);
        if (proposition.getDateLimiteValidite() != null
                && proposition.getDateLimiteValidite().isBefore(LocalDate.now())) {
            throw ApiException.conflit("OFFRE_EXPIREE",
                    "La proposition " + proposition.getId() + " a expire le " + proposition.getDateLimiteValidite());
        }
        transitionner(proposition, StatutProposition.ACCEPTEE, "ACCEPTATION_CANDIDAT",
                Acteur.CANDIDAT.name(), request.commentaire());
        proposition.setDateAcceptation(LocalDate.now());
        propositionRepository.save(proposition);
        employeService.embaucher(proposition, request.dateEmbauche());
        changerStatutCandidature(proposition.getCandidature(), StatutCandidature.EMBAUCHE);
        return mapper.toPropositionDto(proposition);
    }

    public PropositionDto refuser(Long id, ActionRequest request) {
        PropositionOffre proposition = charger(id);
        transitionner(proposition, StatutProposition.REFUSEE, "REFUS_CANDIDAT",
                Acteur.CANDIDAT.name(), request.commentaire());
        propositionRepository.save(proposition);
        changerStatutCandidature(proposition.getCandidature(), StatutCandidature.REFUSE);
        return mapper.toPropositionDto(proposition);
    }

    public PropositionDto annuler(Long id, ActionRequest request) {
        PropositionOffre proposition = charger(id);
        transitionner(proposition, StatutProposition.ANNULEE, "ANNULATION",
                request.acteurOuDefaut(Acteur.RH), request.commentaire());
        propositionRepository.save(proposition);
        return mapper.toPropositionDto(proposition);
    }

    public PropositionOffre creerVersionRevisee(PropositionOffre source, BigDecimal nouveauSalaire,
                                                List<AvantageRequest> avantages, String motif, String auteur) {
        PropositionOffre revision = new PropositionOffre();
        revision.setCandidature(source.getCandidature());
        revision.setVersion(propositionRepository.maxVersionPourCandidature(source.getCandidature().getId()) + 1);
        revision.setPropositionParent(source);
        revision.setSalaireBase(nouveauSalaire != null ? nouveauSalaire : source.getSalaireBase());
        revision.setTempsTravail(source.getTempsTravail());
        revision.setEmploiDuTemps(source.getEmploiDuTemps());
        revision.setLieuTravail(source.getLieuTravail());
        revision.setDatePrisePoste(source.getDatePrisePoste());
        revision.setDateLimiteValidite(source.getDateLimiteValidite());
        revision.setDateEmission(LocalDate.now());
        revision.setDateFinContrat(source.getDateFinContrat());
        revision.setTypeContrat(source.getTypeContrat());
        revision.setPeriodeEssaiMois(source.getPeriodeEssaiMois());
        revision.setPoste(source.getPoste());
        revision.setDepartement(source.getDepartement());
        revision.setStatut(StatutProposition.REVISE);
        revision.setMotifRevision(motif);
        PropositionOffre revisionEnregistree = propositionRepository.save(revision);

        if (avantages != null && !avantages.isEmpty()) {
            ajouterAvantages(revisionEnregistree, avantages);
        } else {
            avantageRepository.findByPropositionIdOrderByOrdreAsc(source.getId())
                    .forEach(sourceAvantage -> ajouterAvantage(revisionEnregistree, sourceAvantage.getType(),
                            sourceAvantage.getLibelle(), sourceAvantage.getDescription(),
                            sourceAvantage.getMontant(), sourceAvantage.getOrdre()));
        }
        historiser(revisionEnregistree, null, StatutProposition.REVISE, "REVISION", auteur, motif);
        return revisionEnregistree;
    }

    public void transitionner(PropositionOffre proposition, StatutProposition cible, String action,
                              String auteur, String commentaire) {
        StatutProposition actuel = proposition.getStatut();
        if (!actuel.peutEvoluerVers(cible)) {
            throw ApiException.conflit("TRANSITION_INVALIDE",
                    "Transition impossible de " + actuel + " vers " + cible + " (transitions autorisees : "
                            + actuel.transitionsAutorisees() + ")");
        }
        proposition.setStatut(cible);
        propositionRepository.save(proposition);
        historiser(proposition, actuel, cible, action, auteur, commentaire);
    }

    public void changerStatutCandidature(Candidature candidature, StatutCandidature statut) {
        candidature.setStatut(statut);
        candidature.setDateStatut(LocalDate.now());
        candidatureRepository.save(candidature);
    }

    public PropositionOffre charger(Long id) {
        return propositionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Proposition introuvable : " + id));
    }

    @Scheduled(cron = "0 15 2 * * *")
    public void expirerPropositions() {
        LocalDate aujourdhui = LocalDate.now();
        List<PropositionOffre> emises = propositionRepository.findByStatutOrderByDateEmissionDesc(
                StatutProposition.EMISE);
        for (PropositionOffre proposition : emises) {
            if (proposition.getDateLimiteValidite() != null
                    && proposition.getDateLimiteValidite().isBefore(aujourdhui)) {
                transitionner(proposition, StatutProposition.EXPIREE, "EXPIRATION", Acteur.SYSTEME.name(),
                        "Validite depassee le " + proposition.getDateLimiteValidite());
                changerStatutCandidature(proposition.getCandidature(), StatutCandidature.ABANDONNE);
            }
        }
    }

    private boolean controlerGrilleSalariale(PropositionOffre proposition, boolean derogation) {
        Long posteId = proposition.getPoste() != null ? proposition.getPoste().getId() : null;
        if (!grilleSalarialeService.postePossedeUneGrille(posteId)) {
            return false;
        }
        GrilleDto grille = grilleSalarialeService.niveauPourSalaire(posteId, proposition.getSalaireBase(),
                LocalDate.now());
        if (grille != null) {
            return false;
        }
        if (derogation) {
            return true;
        }
        StringBuilder message = new StringBuilder("SALAIRE_HORS_GRILLE : le salaire ")
                .append(proposition.getSalaireBase())
                .append(" ne correspond a aucun niveau de la grille du poste ");
        if (proposition.getPoste() != null) {
            message.append('"').append(proposition.getPoste().getTitre()).append('"');
        }
        message.append(" (");
        List<GrilleDto> niveaux = grilleSalarialeService.parPoste(posteId);
        for (int index = 0; index < niveaux.size(); index++) {
            GrilleDto niveau = niveaux.get(index);
            if (index > 0) {
                message.append(", ");
            }
            message.append(niveau.niveau()).append(' ').append(niveau.salaireMin()).append('-').append(niveau.salaireMax());
        }
        message.append("). Une derogation RH avec motif est requise.");
        throw ApiException.conflit("SALAIRE_HORS_GRILLE", message.toString());
    }

    private void controlerDureeCdd(TypeContrat typeContrat, LocalDate datePrisePoste, LocalDate dateFinContrat) {
        if (typeContrat != TypeContrat.CDD) {
            return;
        }
        if (dateFinContrat == null) {
            throw ApiException.badRequest("DATE_FIN_CDD_MANQUANTE",
                    "Un CDD doit comporter une date de fin de contrat");
        }
        LocalDate dateMax = datePrisePoste.plusMonths(properties.getDureeMaxCddMois());
        if (dateFinContrat.isAfter(dateMax)) {
            throw ApiException.badRequest("CDD_TROPS_LONG",
                    "Un CDD ne peut pas depasser " + properties.getDureeMaxCddMois()
                            + " mois (fin maximale : " + dateMax + ")");
        }
    }

    private int periodeEssaiParDefaut(TypeContrat typeContrat) {
        return typeContrat == TypeContrat.CDI ? properties.getPeriodeEssaiDefautMois()
                : (typeContrat == TypeContrat.CDD ? 1 : 0);
    }

    private void ajouterAvantages(PropositionOffre proposition, List<AvantageRequest> avantages) {
        if (avantages == null) {
            return;
        }
        int ordre = 1;
        for (AvantageRequest avantage : avantages) {
            ajouterAvantage(proposition, avantage.type(), avantage.libelle(), avantage.description(),
                    avantage.montant(), ordre++);
        }
    }

    private void ajouterAvantage(PropositionOffre proposition, TypeAvantage type, String libelle,
                                  String description, BigDecimal montant, Integer ordre) {
        AvantageOffre avantage = new AvantageOffre();
        avantage.setProposition(proposition);
        avantage.setType(type != null ? type : TypeAvantage.AUTRE);
        avantage.setLibelle(libelle);
        avantage.setDescription(description);
        avantage.setMontant(montant);
        avantage.setOrdre(ordre != null ? ordre : 0);
        avantageRepository.save(avantage);
    }

    private void historiser(PropositionOffre proposition, StatutProposition ancien, StatutProposition nouveau,
                            String action, String auteur, String commentaire) {
        HistoriqueProposition historique = new HistoriqueProposition();
        historique.setProposition(proposition);
        historique.setAncienStatut(ancien != null ? ancien.name() : null);
        historique.setNouveauStatut(nouveau.name());
        historique.setAction(action);
        historique.setAuteur(auteur);
        historique.setCommentaire(commentaire);
        historique.setDateChangement(LocalDateTime.now());
        historiqueRepository.save(historique);
    }
}
