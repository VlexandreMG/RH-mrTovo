package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.DecisionCongeRequest;
import com.mrtovo.backend.dto.DemandeCongeDto;
import com.mrtovo.backend.dto.TypeCongeDto;
import com.mrtovo.backend.dto.TypeCongeRequest;
import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.DemandeConge;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.StatutDemandeConge;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.TypeConge;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.DemandeCongeRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.TypeCongeRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Bloc 7 : demande de conge -> controle du solde -> avis du manager -> decision du RH. */
@Service
@Transactional
public class CongeService {

    private final DemandeCongeRepository demandeRepository;
    private final TypeCongeRepository typeRepository;
    private final EmployeEmbaucheRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SoldeCongeService soldeService;
    private final DocumentStorageService stockage;
    private final TraceTempsService trace;
    private final Clock horloge;

    @Autowired
    public CongeService(DemandeCongeRepository demandeRepository,
                        TypeCongeRepository typeRepository,
                        EmployeEmbaucheRepository employeRepository,
                        UtilisateurRepository utilisateurRepository,
                        SoldeCongeService soldeService,
                        DocumentStorageService stockage,
                        TraceTempsService trace) {
        this(demandeRepository, typeRepository, employeRepository, utilisateurRepository, soldeService, stockage,
                trace, Clock.systemDefaultZone());
    }

    CongeService(DemandeCongeRepository demandeRepository,
                 TypeCongeRepository typeRepository,
                 EmployeEmbaucheRepository employeRepository,
                 UtilisateurRepository utilisateurRepository,
                 SoldeCongeService soldeService,
                 DocumentStorageService stockage,
                 TraceTempsService trace,
                 Clock horloge) {
        this.demandeRepository = demandeRepository;
        this.typeRepository = typeRepository;
        this.employeRepository = employeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.soldeService = soldeService;
        this.stockage = stockage;
        this.trace = trace;
        this.horloge = horloge;
    }

    // =====================================================================
    // Types de conge
    // =====================================================================

    @Transactional(readOnly = true)
    public List<TypeCongeDto> types() {
        return typeRepository.findAllByOrderByLibelleAsc().stream().map(CongeService::toDto).toList();
    }

    public TypeCongeDto creerType(TypeCongeRequest requete) {
        String code = requete.code().trim().toUpperCase(Locale.ROOT);
        if (typeRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw ApiException.conflit("TYPE_EXISTANT", "Le code " + code + " est déjà utilisé.");
        }
        TypeConge type = new TypeConge();
        type.setCode(code);
        type.setLibelle(requete.libelle().trim());
        type.setDecompteSolde(requete.decompteSolde());
        type.setRemunere(requete.remunere());
        type.setJustificatifRequis(requete.justificatifRequis());
        type.setPlafondAnnuelJours(requete.plafondAnnuelJours());
        return toDto(typeRepository.save(type));
    }

    // =====================================================================
    // Lecture
    // =====================================================================

    @Transactional(readOnly = true)
    public List<DemandeCongeDto> demandesDuSalarie(Long employeId) {
        return demandeRepository.findByEmployeIdOrderByDateDebutDesc(employeId).stream().map(this::toDto).toList();
    }

    /** Le RH voit tout ; un manager ne voit que son departement. */
    @Transactional(readOnly = true)
    public List<DemandeCongeDto> demandesPour(String auteur) {
        Utilisateur lecteur = chargerUtilisateur(auteur);
        return demandeRepository.findAllByOrderByDateDemandeDesc().stream()
                .filter(demande -> peutVoir(lecteur, demande.getEmploye()))
                .map(this::toDto)
                .toList();
    }

    // =====================================================================
    // Parcours d'une demande
    // =====================================================================

    /** Le salarie vient toujours du compte connecte : il ne peut pas demander pour quelqu'un d'autre. */
    public DemandeCongeDto demander(Long employeId, Long typeCongeId, LocalDate debut, LocalDate fin, String motif,
                                    MultipartFile justificatif, String auteur) {
        EmployeEmbauche employe = verrouiller(employeId);
        TypeConge type = typeRepository.findById(typeCongeId)
                .orElseThrow(() -> ApiException.notFound("Type de congé introuvable : " + typeCongeId));

        controlerDates(employe, type, debut, fin);
        int jours = CalculConge.joursCalendaires(debut, fin);
        controlerDisponibilite(employe, type, debut, fin, jours, null);

        DemandeConge demande = new DemandeConge();
        demande.setEmploye(employe);
        demande.setTypeConge(type);
        demande.setDateDebut(debut);
        demande.setDateFin(fin);
        demande.setNbJours(jours);
        demande.setMotif(nettoyer(motif));
        demande.setDemandePar(auteur);
        demande.setStatut(managerExiste(employe)
                ? StatutDemandeConge.EN_ATTENTE_MANAGER
                : StatutDemandeConge.EN_ATTENTE_RH);
        if (justificatif != null && !justificatif.isEmpty()) {
            joindre(demande, justificatif);
        }
        demandeRepository.save(demande);

        trace.tracer(employe, ActionAudit.DEMANDE_CONGE, auteur, decrire(demande));
        return toDto(demande);
    }

    public DemandeCongeDto ajouterJustificatif(Long employeId, Long demandeId, MultipartFile fichier, String auteur) {
        DemandeConge demande = chargerDemandeDuSalarie(employeId, demandeId);
        if (!StatutDemandeConge.ACTIFS.contains(demande.getStatut())) {
            throw ApiException.conflit("DEMANDE_CLOSE", "Cette demande est close.");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw ApiException.badRequest("FICHIER_VIDE", "Le fichier est vide.");
        }
        String ancienneCle = demande.getJustificatifCle();
        joindre(demande, fichier);
        if (ancienneCle != null) {
            DossierSalarieService.apresValidation(() -> stockage.supprimer(ancienneCle));
        }
        trace.tracer(demande.getEmploye(), ActionAudit.AJOUT_JUSTIFICATIF, auteur,
                decrire(demande) + " · fichier : " + demande.getJustificatifNom());
        return toDto(demande);
    }

    public DemandeCongeDto avisManager(Long demandeId, DecisionCongeRequest requete, String auteur) {
        Utilisateur manager = chargerUtilisateur(auteur);
        if (manager.getRole() != RoleUtilisateur.MANAGER) {
            throw ApiException.forbidden("ROLE_MANAGER_REQUIS", "Cette étape revient au manager.");
        }
        DemandeConge demande = chargerDemande(demandeId);
        if (!memeDepartement(manager, demande.getEmploye())) {
            throw ApiException.forbidden("AUTRE_DEPARTEMENT", "Ce salarié n'est pas dans votre département.");
        }
        if (demande.getStatut() != StatutDemandeConge.EN_ATTENTE_MANAGER) {
            throw ApiException.conflit("ETAPE_INVALIDE", "Cette demande n'attend pas l'avis du manager.");
        }
        String commentaire = nettoyer(requete.commentaire());
        exigerMotifDeRefus(requete.accepte(), commentaire);

        demande.setAvisManagerPar(auteur);
        demande.setDateAvisManager(LocalDateTime.now(horloge));
        demande.setCommentaireManager(commentaire);
        demande.setStatut(requete.accepte() ? StatutDemandeConge.EN_ATTENTE_RH : StatutDemandeConge.REFUSEE);
        trace.tracer(demande.getEmploye(), ActionAudit.AVIS_MANAGER_CONGE, auteur,
                decrire(demande) + " : " + (requete.accepte() ? "accord" : "refus")
                        + (commentaire != null ? " — " + commentaire : ""));
        return toDto(demande);
    }

    /** Dernier mot au RH. Le solde et les dates sont revérifies : la situation a pu changer depuis la demande. */
    public DemandeCongeDto decisionRh(Long demandeId, DecisionCongeRequest requete, String auteur) {
        DemandeConge demande = chargerDemande(demandeId);
        EmployeEmbauche employe = verrouiller(demande.getEmploye().getId());

        boolean sansManager = demande.getStatut() == StatutDemandeConge.EN_ATTENTE_MANAGER && !managerExiste(employe);
        if (demande.getStatut() != StatutDemandeConge.EN_ATTENTE_RH && !sansManager) {
            throw ApiException.conflit("ETAPE_INVALIDE", "Cette demande n'attend pas la décision du RH.");
        }
        String commentaire = nettoyer(requete.commentaire());
        exigerMotifDeRefus(requete.accepte(), commentaire);

        if (requete.accepte()) {
            TypeConge type = demande.getTypeConge();
            if (type.isJustificatifRequis() && demande.getJustificatifCle() == null) {
                throw ApiException.conflit("JUSTIFICATIF_MANQUANT",
                        "Un justificatif est obligatoire pour ce type de congé.");
            }
            controlerDisponibilite(employe, type, demande.getDateDebut(), demande.getDateFin(),
                    demande.getNbJours(), demande.getId());
        }

        demande.setDecidePar(auteur);
        demande.setDateDecision(LocalDateTime.now(horloge));
        demande.setCommentaireRh(commentaire);
        demande.setStatut(requete.accepte() ? StatutDemandeConge.VALIDEE : StatutDemandeConge.REFUSEE);
        trace.tracer(employe, ActionAudit.DECISION_RH_CONGE, auteur,
                decrire(demande) + " : " + demande.getStatut().getLibelle()
                        + (commentaire != null ? " — " + commentaire : ""));
        return toDto(demande);
    }

    public DemandeCongeDto annulerParSalarie(Long employeId, Long demandeId, String auteur) {
        return annuler(chargerDemandeDuSalarie(employeId, demandeId), "Annulée par le salarié", auteur);
    }

    public DemandeCongeDto annulerParRh(Long demandeId, String commentaire, String auteur) {
        return annuler(chargerDemande(demandeId), commentaire.trim(), auteur);
    }

    /** Une fois le conge commence, les jours sont consommes : on passe par un ajustement du droit. */
    private DemandeCongeDto annuler(DemandeConge demande, String raison, String auteur) {
        boolean validee = demande.getStatut() == StatutDemandeConge.VALIDEE;
        if (!demande.getStatut().enAttente() && !validee) {
            throw ApiException.conflit("DEMANDE_CLOSE", "Cette demande est déjà close.");
        }
        if (validee && !demande.getDateDebut().isAfter(LocalDate.now(horloge))) {
            throw ApiException.conflit("CONGE_COMMENCE", "Ce congé a commencé : il ne peut plus être annulé.");
        }
        demande.setStatut(StatutDemandeConge.ANNULEE);
        trace.tracer(demande.getEmploye(), ActionAudit.ANNULATION_CONGE, auteur, decrire(demande) + " — " + raison);
        return toDto(demande);
    }

    // =====================================================================
    // Justificatifs
    // =====================================================================

    public DossierSalarieService.FichierTelecharge justificatifPour(Long demandeId, String auteur) {
        DemandeConge demande = chargerDemande(demandeId);
        if (!peutVoir(chargerUtilisateur(auteur), demande.getEmploye())) {
            throw ApiException.forbidden("AUTRE_DEPARTEMENT", "Ce salarié n'est pas dans votre département.");
        }
        return fichier(demande);
    }

    public DossierSalarieService.FichierTelecharge justificatifDuSalarie(Long employeId, Long demandeId) {
        return fichier(chargerDemandeDuSalarie(employeId, demandeId));
    }

    private DossierSalarieService.FichierTelecharge fichier(DemandeConge demande) {
        if (demande.getJustificatifCle() == null) {
            throw ApiException.notFound("Aucun justificatif sur cette demande.");
        }
        return new DossierSalarieService.FichierTelecharge(stockage.charger(demande.getJustificatifCle()),
                demande.getJustificatifType(), demande.getJustificatifNom());
    }

    private void joindre(DemandeConge demande, MultipartFile fichier) {
        DocumentStorageService.FichierStocke stocke = stockage.enregistrer(fichier);
        demande.setJustificatifCle(stocke.cle());
        demande.setJustificatifType(stocke.contentType());
        demande.setJustificatifNom(DossierSalarieService.nomFichierSur(fichier.getOriginalFilename()));
        DossierSalarieService.apresAnnulation(() -> stockage.supprimer(stocke.cle()));
    }

    // =====================================================================
    // Controles
    // =====================================================================

    private void controlerDates(EmployeEmbauche employe, TypeConge type, LocalDate debut, LocalDate fin) {
        if (employe.getStatut() == StatutEmploye.SORTI) {
            throw ApiException.conflit("SALARIE_SORTI", "Ce salarié a quitté les effectifs.");
        }
        if (fin.isBefore(debut)) {
            throw ApiException.badRequest("PERIODE_INVALIDE", "La date de fin précède la date de début.");
        }
        if (debut.isBefore(employe.getDateEntree())) {
            throw ApiException.badRequest("DATE_AVANT_ENTREE", "Le congé commence avant la date d'entrée.");
        }
        // Une maladie se declare apres coup ; un conge se demande a l'avance.
        if (!type.isJustificatifRequis() && debut.isBefore(LocalDate.now(horloge))) {
            throw ApiException.badRequest("DATE_PASSEE", "Ce type de congé doit être demandé à l'avance.");
        }
    }

    /** demandeId exclut la demande elle-meme quand le RH la re-controle au moment de valider. */
    private void controlerDisponibilite(EmployeEmbauche employe, TypeConge type, LocalDate debut, LocalDate fin,
                                        int jours, Long demandeId) {
        List<DemandeConge> chevauchantes = demandeRepository.chevauchantes(employe.getId(), debut, fin,
                        StatutDemandeConge.ACTIFS).stream()
                .filter(demande -> !demande.getId().equals(demandeId))
                .toList();
        if (!chevauchantes.isEmpty()) {
            DemandeConge conflit = chevauchantes.get(0);
            throw ApiException.conflit("CHEVAUCHEMENT", "Ces dates chevauchent une autre demande ("
                    + conflit.getDateDebut() + " → " + conflit.getDateFin() + ").");
        }

        if (type.isDecompteSolde()) {
            BigDecimal disponible = soldeService.solde(employe.getId()).disponible();
            if (demandeId != null) {
                // Une demande en attente est deja deduite du disponible : on la remet avant de comparer.
                disponible = disponible.add(BigDecimal.valueOf(jours));
            }
            if (BigDecimal.valueOf(jours).compareTo(disponible) > 0) {
                throw ApiException.conflit("SOLDE_INSUFFISANT",
                        jours + " jour(s) demandés pour " + disponible + " disponible(s).");
            }
        }

        if (type.getPlafondAnnuelJours() != null) {
            int annee = debut.getYear();
            int dejaPris = demandeRepository.findByEmployeIdOrderByDateDebutDesc(employe.getId()).stream()
                    .filter(demande -> demande.getTypeConge().getId().equals(type.getId()))
                    .filter(demande -> StatutDemandeConge.ACTIFS.contains(demande.getStatut()))
                    .filter(demande -> demande.getDateDebut().getYear() == annee)
                    .filter(demande -> !demande.getId().equals(demandeId))
                    .mapToInt(DemandeConge::getNbJours)
                    .sum();
            if (dejaPris + jours > type.getPlafondAnnuelJours()) {
                throw ApiException.conflit("PLAFOND_DEPASSE", "Plafond de " + type.getPlafondAnnuelJours()
                        + " jours par an : " + dejaPris + " déjà utilisé(s) en " + annee + ".");
            }
        }
    }

    private static void exigerMotifDeRefus(boolean accepte, String commentaire) {
        if (!accepte && commentaire == null) {
            throw ApiException.badRequest("COMMENTAIRE_OBLIGATOIRE", "Un refus doit être justifié.");
        }
    }

    // =====================================================================
    // Outils
    // =====================================================================

    private boolean peutVoir(Utilisateur lecteur, EmployeEmbauche employe) {
        return lecteur.getRole() == RoleUtilisateur.RH || memeDepartement(lecteur, employe);
    }

    /** Les departements du Bloc 1 et du Bloc 3 sont deux tables distinctes : on les relie par leur nom. */
    static boolean memeDepartement(Utilisateur manager, EmployeEmbauche employe) {
        return manager.getDepartement() != null && employe.getDepartement() != null
                && manager.getDepartement().getNom().equalsIgnoreCase(employe.getDepartement().getNom());
    }

    private boolean managerExiste(EmployeEmbauche employe) {
        return employe.getDepartement() != null && utilisateurRepository.existsByRoleAndDepartementNomIgnoreCase(
                RoleUtilisateur.MANAGER, employe.getDepartement().getNom());
    }

    private Utilisateur chargerUtilisateur(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable : " + email));
    }

    private DemandeConge chargerDemande(Long demandeId) {
        return demandeRepository.findById(demandeId)
                .orElseThrow(() -> ApiException.notFound("Demande introuvable : " + demandeId));
    }

    /** 404 et non 403 : un salarie ne doit pas pouvoir deviner les demandes des autres. */
    private DemandeConge chargerDemandeDuSalarie(Long employeId, Long demandeId) {
        return demandeRepository.findById(demandeId)
                .filter(demande -> demande.getEmploye().getId().equals(employeId))
                .orElseThrow(() -> ApiException.notFound("Demande introuvable : " + demandeId));
    }

    private EmployeEmbauche verrouiller(Long employeId) {
        return employeRepository.verrouiller(employeId)
                .orElseThrow(() -> ApiException.notFound("Salarié introuvable : " + employeId));
    }

    private DemandeCongeDto toDto(DemandeConge demande) {
        EmployeEmbauche employe = demande.getEmploye();
        TypeConge type = demande.getTypeConge();
        BigDecimal disponible = demande.getStatut().enAttente() && type.isDecompteSolde()
                ? soldeService.solde(employe.getId()).disponible()
                : null;
        return new DemandeCongeDto(
                demande.getId(),
                employe.getId(),
                employe.getMatricule(),
                employe.getNomComplet(),
                employe.getDepartement() != null ? employe.getDepartement().getNom() : null,
                type.getId(),
                type.getLibelle(),
                type.isDecompteSolde(),
                type.isJustificatifRequis(),
                demande.getDateDebut(),
                demande.getDateFin(),
                demande.getNbJours(),
                demande.getMotif(),
                demande.getStatut(),
                demande.getStatut().getLibelle(),
                demande.getDateDemande(),
                demande.getAvisManagerPar(),
                demande.getCommentaireManager(),
                demande.getDecidePar(),
                demande.getCommentaireRh(),
                demande.getJustificatifCle() != null,
                demande.getJustificatifNom(),
                disponible);
    }

    private static TypeCongeDto toDto(TypeConge type) {
        return new TypeCongeDto(type.getId(), type.getCode(), type.getLibelle(), type.isDecompteSolde(),
                type.isRemunere(), type.isJustificatifRequis(), type.getPlafondAnnuelJours());
    }

    private static String decrire(DemandeConge demande) {
        return demande.getTypeConge().getLibelle() + " du " + demande.getDateDebut() + " au " + demande.getDateFin()
                + " (" + demande.getNbJours() + " j)";
    }

    private static String nettoyer(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
