package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.ApiDtos.*;
import com.mrtovo.backend.entity.*;
import com.mrtovo.backend.exception.BusinessException;
import com.mrtovo.backend.model.OffreEmploi;
import com.mrtovo.backend.model.Statuts;
import com.mrtovo.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DemandeService {

    private final DemandeRecrutementRepository demandeRepository;
    private final DepartementRepository departementRepository;
    private final PosteRepository posteRepository;
    private final NiveauPosteRepository niveauRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final OffreEmploiRepository offreRepository;
    private final StatutOffreEmploiRepository statutOffreRepository;

    public DemandeService(DemandeRecrutementRepository demandeRepository,
                          DepartementRepository departementRepository,
                          PosteRepository posteRepository,
                          NiveauPosteRepository niveauRepository,
                          UtilisateurRepository utilisateurRepository,
                          EmployeRepository employeRepository,
                          OffreEmploiRepository offreRepository,
                          StatutOffreEmploiRepository statutOffreRepository) {
        this.demandeRepository = demandeRepository;
        this.departementRepository = departementRepository;
        this.posteRepository = posteRepository;
        this.niveauRepository = niveauRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.employeRepository = employeRepository;
        this.offreRepository = offreRepository;
        this.statutOffreRepository = statutOffreRepository;
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
        demande.setStatut(StatutDemande.EN_ATTENTE_DIRECTION);
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

        DemandeRecrutement enregistree = demandeRepository.save(demande);
        creerOffre(enregistree);
        return toDto(enregistree);
    }

    /** Crée les offres des demandes qui n'en ont pas encore (demandes antérieures à ce lien). */
    @Transactional
    public void creerOffresManquantes() {
        for (DemandeRecrutement d : demandeRepository.findAll()) {
            if (offreRepository.findByDemandeIdDemande(d.getIdDemande()).isEmpty()) {
                creerOffre(d);
                if (d.getStatut() == StatutDemande.RECRUTEMENT_OUVERT) {
                    changerStatutOffre(d, Statuts.OffreEmploi.OUVERTE);
                } else if (d.getStatut() == StatutDemande.REFUSEE) {
                    changerStatutOffre(d, Statuts.OffreEmploi.FERMEE);
                }
            }
        }
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
     * Seule la Direction accepte ou refuse la demande. Le RH et la Finance sont en lecture seule :
     * ils consultent le dossier mais ne peuvent plus le valider.
     */
    @Transactional
    public DemandeDto valider(Long idDemande, ValidationRequest req, String email) {
        Utilisateur validateur = utilisateur(email);
        if (validateur.getRole() != RoleUtilisateur.DIRECTION) {
            throw new BusinessException("Seule la Direction peut accepter ou refuser une demande de recrutement.");
        }
        DemandeRecrutement demande = demande(idDemande);

        boolean enAttente = switch (demande.getStatut()) {
            case EN_ATTENTE_RH, EN_ATTENTE_FINANCE, EN_ATTENTE_DIRECTION -> true;
            default -> false;
        };
        if (!enAttente) {
            throw new BusinessException("Cette demande n'est plus en cours de validation.");
        }
        boolean dejaFait = demande.getValidations().stream()
                .anyMatch(v -> v.getEtape() == EtapeValidation.DIRECTION);
        if (dejaFait) {
            throw new BusinessException("La Direction a déjà traité cette demande.");
        }

        DecisionValidation decision;
        try {
            decision = DecisionValidation.valueOf(String.valueOf(req.decision()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("La décision doit être FAVORABLE ou DEFAVORABLE.");
        }
        String commentaire = req.commentaire() == null ? "" : req.commentaire().trim();
        if (decision == DecisionValidation.DEFAVORABLE && commentaire.isEmpty()) {
            throw new BusinessException("Un commentaire est obligatoire pour une décision défavorable.");
        }

        Validation validation = new Validation();
        validation.setDemande(demande);
        validation.setValidateur(validateur);
        validation.setEtape(EtapeValidation.DIRECTION);
        validation.setDecision(decision);
        validation.setCommentaire(commentaire.isEmpty() ? null : commentaire);
        validation.setDateValidation(LocalDateTime.now());
        demande.getValidations().add(validation);

        demande.setStatut(decision == DecisionValidation.FAVORABLE
                ? StatutDemande.AUTORISEE : StatutDemande.REFUSEE);
        if (demande.getStatut() == StatutDemande.REFUSEE) {
            changerStatutOffre(demande, Statuts.OffreEmploi.FERMEE);
        }

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
        changerStatutOffre(d, Statuts.OffreEmploi.OUVERTE);
        return toDto(demandeRepository.save(d));
    }

    // ------------------------------------------------------------------ Offre d'emploi liée

    /** L'offre est créée avec la demande, en attente : elle n'est publiée qu'à l'ouverture du recrutement. */
    private OffreEmploi creerOffre(DemandeRecrutement d) {
        StringBuilder description = new StringBuilder();
        if (d.getPoste().getDescription() != null) {
            description.append(d.getPoste().getDescription()).append("\n\n");
        }
        description.append(d.getMotif()).append("\n\nPostes à pourvoir : ");
        description.append(String.join(", ", d.getLignes().stream()
                .map(l -> l.getEffectif() + " " + l.getNiveau().getLibelle())
                .toList()));
        description.append(".");

        OffreEmploi offre = new OffreEmploi();
        offre.setDemande(d);
        offre.setTitre(d.getPoste().getIntitule() + " – " + d.getDepartement().getNom());
        offre.setDescription(description.toString());
        offre.setDatePublication(d.getDateCreation().toLocalDate());
        offre.setStatutOffreEmploi(statutOffreRepository.getReferenceById(Statuts.OffreEmploi.EN_ATTENTE));
        return offreRepository.save(offre);
    }

    private void changerStatutOffre(DemandeRecrutement d, long idStatut) {
        offreRepository.findByDemandeIdDemande(d.getIdDemande()).ifPresent(offre -> {
            offre.setStatutOffreEmploi(statutOffreRepository.getReferenceById(idStatut));
            if (idStatut == Statuts.OffreEmploi.OUVERTE) {
                offre.setDatePublication(LocalDate.now());
            }
        });
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
        OffreEmploi offre = offreRepository.findByDemandeIdDemande(d.getIdDemande()).orElse(null);
        return new DemandeDto(d.getIdDemande(),
                d.getPoste().getIdPoste(), d.getPoste().getIntitule(),
                d.getDepartement().getIdDepartement(), d.getDepartement().getNom(),
                d.getManager().getNom(),
                d.getTypeBesoin().name(), d.getMotif(),
                e == null ? null : e.getNom() + " (" + e.getMatricule() + ")",
                d.getBudgetDemandeTotal(), effectifTotal, coutTotal,
                d.getDateSouhaitee(), d.getStatut().name(),
                d.getDateCreation(), d.getDateOuverture(),
                lignes, validations,
                offre == null ? null : offre.getId(),
                offre == null || offre.getStatutOffreEmploi() == null ? null : offre.getStatutOffreEmploi().getLibelle());
    }
}
