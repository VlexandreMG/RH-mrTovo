package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.ApiDtos.*;
import com.mrtovo.backend.entity.*;
import com.mrtovo.backend.exception.BusinessException;
import com.mrtovo.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DemandeService {

    private final DemandeRecrutementRepository demandeRepository;
    private final DepartementRepository departementRepository;
    private final PosteRepository posteRepository;
    private final NiveauPosteRepository niveauRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final GrilleService grilleService;

    public DemandeService(DemandeRecrutementRepository demandeRepository,
                          DepartementRepository departementRepository,
                          PosteRepository posteRepository,
                          NiveauPosteRepository niveauRepository,
                          UtilisateurRepository utilisateurRepository,
                          EmployeRepository employeRepository,
                          GrilleService grilleService) {
        this.demandeRepository = demandeRepository;
        this.departementRepository = departementRepository;
        this.posteRepository = posteRepository;
        this.niveauRepository = niveauRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.employeRepository = employeRepository;
        this.grilleService = grilleService;
    }

    // ------------------------------------------------------------------ Création

    @Transactional
    public DemandeDto creer(DemandeRecrutementRequest req, String email) {
        Utilisateur manager = utilisateur(email);
        if (manager.getRole() != RoleUtilisateur.MANAGER) {
            throw new BusinessException("Seul un manager peut créer une demande.");
        }
        if (req.idPoste() == null) throw new BusinessException("Le poste est obligatoire.");
Poste poste = posteRepository.findById(req.idPoste())
        .orElseThrow(() -> new BusinessException("Poste introuvable."));
// Le département est toujours celui du manager qui crée la demande
Departement departement = manager.getDepartement();
if (departement == null) {
    throw new BusinessException("Votre compte n'est rattaché à aucun département.");
}

        TypeBesoin type;
        try {
            type = TypeBesoin.valueOf(String.valueOf(req.typeBesoin()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Le type de besoin doit être CREATION ou REMPLACEMENT.");
        }
        if (req.motif() == null || req.motif().trim().isEmpty()) {
            throw new BusinessException("Le motif est obligatoire : expliquez pourquoi ces personnes sont nécessaires.");
        }
        if (req.budgetDemandeTotal() == null || req.budgetDemandeTotal().signum() < 0) {
            throw new BusinessException("Le budget demandé doit être un montant positif ou nul.");
        }
        if (req.dateSouhaitee() == null || req.dateSouhaitee().isBefore(LocalDate.now())) {
            throw new BusinessException("La date souhaitée doit être aujourd'hui ou plus tard.");
        }
        if (req.lignes() == null || req.lignes().isEmpty()) {
            throw new BusinessException("Ajoutez au moins un niveau de poste avec son effectif.");
        }

        DemandeRecrutement demande = new DemandeRecrutement();
        demande.setPoste(poste);
        demande.setDepartement(departement);
        demande.setManager(manager);
        demande.setTypeBesoin(type);
        demande.setMotif(req.motif().trim());
        demande.setBudgetDemandeTotal(req.budgetDemandeTotal());
        demande.setDateSouhaitee(req.dateSouhaitee());
        demande.setStatut(StatutDemande.EN_ATTENTE_RH);
        demande.setDateCreation(LocalDateTime.now());

        if (type == TypeBesoin.REMPLACEMENT && req.idEmployeARemplacer() != null) {
            Employe employe = employeRepository.findById(req.idEmployeARemplacer())
                    .orElseThrow(() -> new BusinessException("Employé à remplacer introuvable."));
            if (!employe.getPoste().getIdPoste().equals(poste.getIdPoste())) {
                throw new BusinessException("L'employé à remplacer n'occupe pas ce poste.");
            }
            demande.setEmployeARemplacer(employe);
        }

        Set<Long> niveauxVus = new HashSet<>();
        for (LigneRequest lr : req.lignes()) {
            if (lr.idNiveau() == null) throw new BusinessException("Chaque ligne doit avoir un niveau.");
            if (lr.effectif() == null || lr.effectif() <= 0) {
                throw new BusinessException("L'effectif de chaque ligne doit être supérieur à 0.");
            }
            if (!niveauxVus.add(lr.idNiveau())) {
                throw new BusinessException("Un même niveau ne peut apparaître qu'une seule fois : regroupez les effectifs.");
            }
            NiveauPoste niveau = niveauRepository.findById(lr.idNiveau())
                    .orElseThrow(() -> new BusinessException("Niveau de poste introuvable."));
            LigneDemande ligne = new LigneDemande();
            ligne.setDemande(demande);
            ligne.setNiveau(niveau);
            ligne.setEffectif(lr.effectif());
            demande.getLignes().add(ligne);
        }

        return toDto(demandeRepository.save(demande));
    }

    // ------------------------------------------------------------------ Consultation

    @Transactional(readOnly = true)
    public List<DemandeDto> lister(String email) {
        Utilisateur u = utilisateur(email);
        List<DemandeRecrutement> demandes = (u.getRole() == RoleUtilisateur.MANAGER)
        ? demandeRepository.findByDepartementOrderByDateCreationDesc(u.getDepartement())
        : demandeRepository.findAllByOrderByDateCreationDesc();
        return demandes.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DemandeDto detail(Long id, String email) {
        Utilisateur u = utilisateur(email);
        DemandeRecrutement d = demande(id);
     if (u.getRole() == RoleUtilisateur.MANAGER
        && (u.getDepartement() == null
            || !d.getDepartement().getIdDepartement().equals(u.getDepartement().getIdDepartement()))) {
    throw new BusinessException("Vous ne pouvez consulter que les demandes de votre département.");
}
        return toDto(d);
    }

    // ------------------------------------------------------------------ Workflow

    /**
     * RH → Finance → Direction. RH et Finance donnent un AVIS (même défavorable, la demande continue) ;
     * la Direction rend la décision finale sur la demande entière.
     */
    @Transactional
    public DemandeDto valider(Long idDemande, ValidationRequest req, String email) {
        Utilisateur validateur = utilisateur(email);
        DemandeRecrutement demande = demande(idDemande);

        EtapeValidation etape = switch (demande.getStatut()) {
            case EN_ATTENTE_RH -> EtapeValidation.RH;
            case EN_ATTENTE_FINANCE -> EtapeValidation.FINANCE;
            case EN_ATTENTE_DIRECTION -> EtapeValidation.DIRECTION;
            default -> throw new BusinessException("Cette demande n'est plus en cours de validation.");
        };
        if (!validateur.getRole().name().equals(etape.name())) {
            throw new BusinessException("Cette étape est réservée au rôle " + etape + ".");
        }
        boolean dejaFait = demande.getValidations().stream().anyMatch(v -> v.getEtape() == etape);
        if (dejaFait) {
            throw new BusinessException("Cette étape a déjà été traitée pour cette demande.");
        }

        DecisionValidation decision;
        try {
            decision = DecisionValidation.valueOf(String.valueOf(req.decision()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("La décision doit être FAVORABLE ou DEFAVORABLE.");
        }
        String commentaire = req.commentaire() == null ? "" : req.commentaire().trim();
        if (decision == DecisionValidation.DEFAVORABLE && commentaire.isEmpty()) {
            throw new BusinessException("Un commentaire est obligatoire pour un avis défavorable.");
        }

        if (etape == EtapeValidation.RH) {
            estimer(demande, req.estimations(), decision == DecisionValidation.FAVORABLE);
        }

        Validation validation = new Validation();
        validation.setDemande(demande);
        validation.setValidateur(validateur);
        validation.setEtape(etape);
        validation.setDecision(decision);
        validation.setCommentaire(commentaire.isEmpty() ? null : commentaire);
        validation.setDateValidation(LocalDateTime.now());
        demande.getValidations().add(validation);

        demande.setStatut(switch (etape) {
            case RH -> StatutDemande.EN_ATTENTE_FINANCE;
            case FINANCE -> StatutDemande.EN_ATTENTE_DIRECTION;
            case DIRECTION -> decision == DecisionValidation.FAVORABLE
                    ? StatutDemande.AUTORISEE : StatutDemande.REFUSEE;
        });

        return toDto(demandeRepository.save(demande));
    }

    @Transactional
    public DemandeDto ouvrir(Long idDemande, String email) {
        Utilisateur u = utilisateur(email);
        if (u.getRole() != RoleUtilisateur.RH) {
            throw new BusinessException("Seul le RH peut ouvrir le recrutement.");
        }
        DemandeRecrutement d = demande(idDemande);
        if (d.getStatut() != StatutDemande.AUTORISEE) {
            throw new BusinessException("Le recrutement ne peut être ouvert que si la Direction a autorisé la demande.");
        }
        d.setStatut(StatutDemande.RECRUTEMENT_OUVERT);
        d.setDateOuverture(LocalDateTime.now());
        return toDto(demandeRepository.save(d));
    }

    /**
     * Estimation du RH, ligne par ligne.
     * - CREATION : le RH peut définir une nouvelle fourchette dans la grille (ou reprendre la grille en vigueur).
     * - REMPLACEMENT : on référence obligatoirement la grille déjà existante.
     */
    private void estimer(DemandeRecrutement demande, List<EstimationRequest> estimations, boolean obligatoire) {
        Map<Long, EstimationRequest> parLigne = new HashMap<>();
        if (estimations != null) {
            for (EstimationRequest e : estimations) {
                if (e.idLigne() != null) parLigne.put(e.idLigne(), e);
            }
        }
        for (LigneDemande ligne : demande.getLignes()) {
            String nomNiveau = ligne.getNiveau().getLibelle();
            EstimationRequest est = parLigne.get(ligne.getIdLigne());
            if (est == null || est.coutEstimeParPersonne() == null) {
                if (obligatoire) {
                    throw new BusinessException("Renseignez le coût estimé par personne pour le niveau « " + nomNiveau + " ».");
                }
                continue;
            }
            if (est.coutEstimeParPersonne().signum() < 0) {
                throw new BusinessException("Le coût estimé ne peut pas être négatif (niveau « " + nomNiveau + " »).");
            }

            GrilleSalariale grille;
            boolean nouvelleFourchette = est.salaireMin() != null || est.salaireMax() != null;
            if (nouvelleFourchette) {
                if (demande.getTypeBesoin() != TypeBesoin.CREATION) {
                    throw new BusinessException("Pour un remplacement, on utilise la grille salariale existante : aucune nouvelle fourchette possible.");
                }
                if (est.salaireMin() == null || est.salaireMax() == null) {
                    throw new BusinessException("Renseignez le salaire minimum ET maximum (niveau « " + nomNiveau + " »).");
                }
                grille = grilleService.definir(demande.getPoste(), ligne.getNiveau(), est.salaireMin(), est.salaireMax());
            } else {
                grille = grilleService.applicable(demande.getPoste(), ligne.getNiveau(), LocalDate.now())
                        .orElseThrow(() -> new BusinessException(
                                demande.getTypeBesoin() == TypeBesoin.CREATION
                                        ? "Aucune grille pour « " + nomNiveau + " » : définissez une fourchette salariale."
                                        : "Aucune grille salariale existante pour le niveau « " + nomNiveau + " »."));
            }

            BigDecimal cout = est.coutEstimeParPersonne();
            if (cout.compareTo(grille.getSalaireMin()) < 0 || cout.compareTo(grille.getSalaireMax()) > 0) {
                throw new BusinessException("Le coût estimé du niveau « " + nomNiveau + " » doit être compris entre "
                        + grille.getSalaireMin().toPlainString() + " et " + grille.getSalaireMax().toPlainString() + " Ar.");
            }
            ligne.setGrille(grille);
            ligne.setCoutEstimeParPersonne(cout);
        }
    }

    // ------------------------------------------------------------------ Utilitaires

    private Utilisateur utilisateur(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
    }

    private DemandeRecrutement demande(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Demande introuvable."));
    }

    private DemandeDto toDto(DemandeRecrutement d) {
        List<LigneDto> lignes = d.getLignes().stream().map(l -> {
            GrilleSalariale g = l.getGrille();
            BigDecimal total = l.getCoutEstimeParPersonne() == null ? null
                    : l.getCoutEstimeParPersonne().multiply(BigDecimal.valueOf(l.getEffectif()));
            return new LigneDto(l.getIdLigne(), l.getNiveau().getIdNiveau(), l.getNiveau().getLibelle(),
                    l.getEffectif(), g == null ? null : g.getIdGrille(),
                    g == null ? null : g.getSalaireMin(), g == null ? null : g.getSalaireMax(),
                    l.getCoutEstimeParPersonne(), total);
        }).toList();

        int effectifTotal = lignes.stream().mapToInt(LigneDto::effectif).sum();
        boolean estime = lignes.stream().anyMatch(l -> l.coutEstimeTotal() != null);
        BigDecimal coutTotal = estime
                ? lignes.stream().map(LigneDto::coutEstimeTotal).filter(x -> x != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;

        List<ValidationDto> validations = d.getValidations().stream()
                .map(v -> new ValidationDto(v.getIdValidation(), v.getEtape().name(), v.getDecision().name(),
                        v.getCommentaire(), v.getValidateur().getNom(), v.getValidateur().getRole().name(),
                        v.getDateValidation()))
                .toList();

        Employe e = d.getEmployeARemplacer();
        return new DemandeDto(d.getIdDemande(),
                d.getPoste().getIdPoste(), d.getPoste().getIntitule(),
                d.getDepartement().getIdDepartement(), d.getDepartement().getNom(),
                d.getManager().getNom(),
                d.getTypeBesoin().name(), d.getMotif(),
                e == null ? null : e.getNom() + " (" + e.getMatricule() + ")",
                d.getBudgetDemandeTotal(), effectifTotal, coutTotal,
                d.getDateSouhaitee(), d.getStatut().name(),
                d.getDateCreation(), d.getDateOuverture(),
                lignes, validations);
    }
}
