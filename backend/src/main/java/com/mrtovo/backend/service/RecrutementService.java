package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.CandidatureDTO;
import com.mrtovo.backend.dto.HistoriqueCandidatureDTO;
import com.mrtovo.backend.dto.OffreDTO;
import com.mrtovo.backend.exception.RegleMetierException;
import com.mrtovo.backend.exception.RessourceIntrouvableException;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.model.*;
import com.mrtovo.backend.model.Candidat;
import com.mrtovo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import org.springframework.web.multipart.MultipartFile;

/**
 * Parcours d'une candidature :
 *   À traiter --présélection--> Test --décision test--> Attente entretien --résultat entretien--> Accepté
 *        \--rejet--> Refusé       \--échec--> Refusé                          \--refus--> Refusé
 * Une candidature "Accepté" est transmise à Sarobidy pour la suite (embauche).
 * Chaque action ajoute une ligne à l'historique de la candidature : qui, quoi, quand.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RecrutementService {

    private final OffreEmploiRepository offreRepo;
    private final CandidatureRepository candidatureRepo;
    private final EvaluationRepository evaluationRepo;
    private final EntretienRepository entretienRepo;
    private final StatutCandidatureRepository statutCandidatureRepo;
    private final StatutEvaluationRepository statutEvaluationRepo;
    private final StatutEntretienRepository statutEntretienRepo;
    private final CandidatRepository candidatRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final CvStorageService cvStorageService;
    private final HistoriqueCandidatureRepository historiqueRepo;

    private static final DateTimeFormatter FORMAT_ENTRETIEN = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    // ------------------------------------------------------------------ Lecture

    @Transactional(readOnly = true)
    public List<OffreDTO> listerOffres() {
        return offreRepo.findAll().stream().map(this::versDTO).toList();
    }

    @Transactional(readOnly = true)
    public OffreDTO getOffre(Long id) {
        return versDTO(offreRepo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Offre #" + id + " introuvable")));
    }

    @Transactional(readOnly = true)
    public List<OffreDTO> listerOffresPubliques() {
        return offreRepo.findByStatutOffreEmploiIdOrderByDatePublicationDesc(Statuts.OffreEmploi.OUVERTE)
                .stream().map(this::versDTO).toList();
    }

    @Transactional(readOnly = true)
    public OffreDTO getOffrePublique(Long id) {
        OffreEmploi offre = offreRepo.findById(id)
                .filter(candidate -> candidate.getStatutOffreEmploi() != null
                        && candidate.getStatutOffreEmploi().getId() == Statuts.OffreEmploi.OUVERTE)
                .orElseThrow(() -> new RessourceIntrouvableException("Offre introuvable ou non ouverte."));
        return versDTO(offre);
    }

    public CandidatureDTO postuler(Long offreId, String nom, String prenom, String email,
                                   String telephone, MultipartFile cv) {
        OffreEmploi offre = offreRepo.findById(offreId)
                .filter(candidate -> candidate.getStatutOffreEmploi() != null
                        && candidate.getStatutOffreEmploi().getId() == Statuts.OffreEmploi.OUVERTE)
                .orElseThrow(() -> new RessourceIntrouvableException("Offre introuvable ou non ouverte."));

        String emailNormalise = email.trim().toLowerCase(java.util.Locale.ROOT);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CANDIDAT"))
                && !authentication.getName().equalsIgnoreCase(emailNormalise)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Connectez-vous avec l'adresse e-mail de votre compte candidat.");
        }

        Utilisateur compte = utilisateurRepo.findByEmailIgnoreCase(emailNormalise).orElse(null);
        if (compte != null && compte.getRole() == RoleUtilisateur.CANDIDAT
                && (authentication == null || !authentication.isAuthenticated()
                || !authentication.getName().equalsIgnoreCase(emailNormalise)
                || authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_CANDIDAT")))) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Connectez-vous à votre compte candidat pour postuler avec cette adresse.");
        }

        Candidat candidat = candidatRepo.findByEmailIgnoreCase(emailNormalise).orElseGet(Candidat::new);
        if (candidat.getId() != null
                && candidatureRepo.existsByCandidatIdAndOffreEmploiId(candidat.getId(), offreId)) {
            throw new RegleMetierException("Une candidature existe déjà pour cette adresse et cette offre.");
        }

        if (compte == null || compte.getRole() != RoleUtilisateur.CANDIDAT) {
            candidat.setNom(nom.trim());
            candidat.setPrenom(prenom.trim());
            candidat.setEmail(emailNormalise);
            candidat.setTelephone(telephone == null || telephone.isBlank() ? null : telephone.trim());
        }
        candidat = candidatRepo.save(candidat);
        candidat.setFichierCvUrl(cvStorageService.enregistrer(cv));

        Candidature candidature = new Candidature();
        candidature.setCandidat(candidat);
        candidature.setOffreEmploi(offre);
        candidature.setDateCandidature(LocalDate.now());
        candidature.setStatutCandidature(
                statutCandidatureRepo.findById(Statuts.Candidature.A_TRAITER).orElseThrow());
        candidatureRepo.save(candidature);
        // Le depot est public : l'auteur est le candidat lui-meme, identifie par son email.
        tracer(candidature, ActionCandidature.CANDIDATURE_DEPOSEE, null, candidature.getStatutCandidature(),
                emailNormalise, "Offre « " + offre.getTitre() + " »");
        return versDTO(candidature);
    }

    @Transactional(readOnly = true)
    public CvStorageService.StoredCv cvCandidature(Long candidatureId) {
        Candidature candidature = chargerCandidature(candidatureId);
        return cvStorageService.charger(candidature.getCandidat().getFichierCvUrl());
    }

    @Transactional(readOnly = true)
    public List<CandidatureDTO> candidaturesDeLOffre(Long offreId) {
        if (!offreRepo.existsById(offreId)) {
            throw new RessourceIntrouvableException("Offre #" + offreId + " introuvable");
        }
        return candidatureRepo.findByOffreEmploiIdOrderByDateCandidatureAscIdAsc(offreId)
                .stream().map(this::versDTO).toList();
    }

    /** Candidats validés après entretien, à transmettre à Sarobidy. */
    @Transactional(readOnly = true)
    public List<CandidatureDTO> candidaturesAcceptees() {
        return candidatureRepo.findByStatutCandidatureIdOrderByIdAsc(Statuts.Candidature.ACCEPTE)
                .stream().map(this::versDTO).toList();
    }

    @Transactional(readOnly = true)
    public CandidatureDTO getCandidature(Long id) {
        return versDTO(chargerCandidature(id));
    }

    @Transactional(readOnly = true)
    public List<HistoriqueCandidatureDTO> historique(Long candidatureId) {
        chargerCandidature(candidatureId);
        return historiqueRepo.findByCandidatureIdOrderByDateActionAscIdAsc(candidatureId).stream()
                .map(RecrutementService::historiqueVersDTO)
                .toList();
    }

    // ------------------------------------------------------------ 1. Présélection

    public CandidatureDTO preselectionner(Long candidatureId, boolean retenue) {
        Candidature c = chargerCandidature(candidatureId);
        verifierStatut(c, Statuts.Candidature.A_TRAITER, "être présélectionnée");

        if (retenue) {
            Evaluation test = new Evaluation();
            test.setCandidature(c);
            test.setStatutEvaluation(statutEvaluationRepo.getReferenceById(Statuts.Evaluation.EN_ATTENTE));
            evaluationRepo.save(test);
            changerStatut(c, Statuts.Candidature.TEST, ActionCandidature.PRESELECTION_RETENUE, null);
        } else {
            changerStatut(c, Statuts.Candidature.REFUSE, ActionCandidature.PRESELECTION_REJETEE, null);
        }
        return versDTO(c);
    }

    // ------------------------------------------------------------------- 2. Test

    /** Saisie (ou correction) de la note : ne fait pas avancer la candidature. */
    public CandidatureDTO saisirNote(Long candidatureId, BigDecimal note) {
        Candidature c = chargerCandidature(candidatureId);
        verifierStatut(c, Statuts.Candidature.TEST, "recevoir une note de test");
        Evaluation test = chargerTest(c);
        BigDecimal ancienne = test.getNoteTest();
        if (ancienne != null && ancienne.compareTo(note) == 0) {
            return versDTO(c);
        }
        test.setNoteTest(note);
        if (ancienne == null) {
            tracer(c, ActionCandidature.NOTE_SAISIE, null, null, auteurCourant(), note(note));
        } else {
            tracer(c, ActionCandidature.NOTE_CORRIGEE, null, null, auteurCourant(),
                    note(ancienne) + " → " + note(note));
        }
        return versDTO(c);
    }

    /** Le recruteur valide ou refuse le test après avoir vu la note. */
    public CandidatureDTO deciderTest(Long candidatureId, boolean valide) {
        Candidature c = chargerCandidature(candidatureId);
        verifierStatut(c, Statuts.Candidature.TEST, "faire l'objet d'une décision de test");
        Evaluation test = chargerTest(c);
        if (test.getNoteTest() == null) {
            throw new RegleMetierException("Saisissez la note du test avant de prendre une décision");
        }
        if (valide) {
            test.setStatutEvaluation(statutEvaluationRepo.getReferenceById(Statuts.Evaluation.VALIDE));
            changerStatut(c, Statuts.Candidature.ATTENTE_ENTRETIEN, ActionCandidature.TEST_VALIDE,
                    note(test.getNoteTest()));
        } else {
            test.setStatutEvaluation(statutEvaluationRepo.getReferenceById(Statuts.Evaluation.ECHOUE));
            changerStatut(c, Statuts.Candidature.REFUSE, ActionCandidature.TEST_ECHOUE, note(test.getNoteTest()));
        }
        return versDTO(c);
    }

    // -------------------------------------------------------------- 3. Entretien

    public CandidatureDTO planifierEntretien(Long candidatureId, LocalDateTime date, String lieu) {
        Candidature c = chargerCandidature(candidatureId);
        verifierStatut(c, Statuts.Candidature.ATTENTE_ENTRETIEN, "avoir un entretien planifié");
        Evaluation test = chargerTest(c);
        if (entretienRepo.existsByEvaluationIdAndStatutEntretienId(test.getId(), Statuts.Entretien.PLANIFIE)) {
            throw new RegleMetierException("Un entretien est déjà planifié pour cette candidature");
        }
        Entretien e = new Entretien();
        e.setEvaluation(test);
        e.setDateEntretien(date);
        e.setLieu(lieu);
        e.setStatutEntretien(statutEntretienRepo.getReferenceById(Statuts.Entretien.PLANIFIE));
        entretienRepo.save(e);
        tracer(c, ActionCandidature.ENTRETIEN_PLANIFIE, null, null, auteurCourant(), decrireEntretien(e));
        return versDTO(c);
    }

    public CandidatureDTO annulerEntretien(Long entretienId) {
        Entretien e = chargerEntretienPlanifie(entretienId);
        e.setStatutEntretien(statutEntretienRepo.getReferenceById(Statuts.Entretien.ANNULE));
        Candidature c = e.getEvaluation().getCandidature();
        tracer(c, ActionCandidature.ENTRETIEN_ANNULE, null, null, auteurCourant(), decrireEntretien(e));
        return versDTO(c);
    }

    /** Résultat de l'entretien : accepté (→ Sarobidy) ou refusé. */
    public CandidatureDTO resultatEntretien(Long entretienId, boolean accepte, String commentaire) {
        Entretien e = chargerEntretienPlanifie(entretienId);
        Candidature c = e.getEvaluation().getCandidature();
        verifierStatut(c, Statuts.Candidature.ATTENTE_ENTRETIEN, "recevoir un résultat d'entretien");
        e.setCommentaire(commentaire);
        e.setStatutEntretien(statutEntretienRepo.getReferenceById(Statuts.Entretien.TERMINE));
        changerStatut(c, accepte ? Statuts.Candidature.ACCEPTE : Statuts.Candidature.REFUSE,
                accepte ? ActionCandidature.CANDIDAT_ACCEPTE : ActionCandidature.CANDIDAT_REFUSE,
                commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
        return versDTO(c);
    }

    // ---------------------------------------------------------------- Outils

    private Candidature chargerCandidature(Long id) {
        return candidatureRepo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Candidature #" + id + " introuvable"));
    }

    private Evaluation chargerTest(Candidature c) {
        return evaluationRepo.findByCandidatureId(c.getId())
                .orElseThrow(() -> new RegleMetierException("Aucun test associé à la candidature #" + c.getId()));
    }

    private Entretien chargerEntretienPlanifie(Long id) {
        Entretien e = entretienRepo.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Entretien #" + id + " introuvable"));
        if (!e.getStatutEntretien().getId().equals(Statuts.Entretien.PLANIFIE)) {
            throw new RegleMetierException("Cet entretien est déjà " + e.getStatutEntretien().getLibelle().toLowerCase());
        }
        return e;
    }

    private void verifierStatut(Candidature c, long attendu, String action) {
        if (c.getStatutCandidature() == null || !c.getStatutCandidature().getId().equals(attendu)) {
            String actuel = c.getStatutCandidature() == null ? "sans statut" : c.getStatutCandidature().getLibelle();
            throw new RegleMetierException("La candidature #" + c.getId() + " est au statut « " + actuel
                    + " » et ne peut pas " + action);
        }
    }

    /** Seul point de changement de statut : il ne peut donc pas y en avoir un sans sa ligne d'historique. */
    private void changerStatut(Candidature c, long statutId, ActionCandidature action, String details) {
        StatutCandidature ancien = c.getStatutCandidature();
        StatutCandidature nouveau = statutCandidatureRepo.findById(statutId).orElseThrow();
        c.setStatutCandidature(nouveau);
        tracer(c, action, ancien, nouveau, auteurCourant(), details);
    }

    private void tracer(Candidature c, ActionCandidature action, StatutCandidature ancien,
                        StatutCandidature nouveau, String auteur, String details) {
        HistoriqueCandidature ligne = new HistoriqueCandidature();
        ligne.setCandidature(c);
        ligne.setAction(action);
        ligne.setAncienStatut(ancien);
        ligne.setNouveauStatut(nouveau);
        ligne.setAuteur(auteur);
        ligne.setDetails(details);
        ligne.setDateAction(LocalDateTime.now());
        historiqueRepo.save(ligne);
    }

    /** L'auteur vient du compte connecte, jamais de la requete. */
    private static String auteurCourant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "inconnu";
        }
        return authentication.getName();
    }

    private static String note(BigDecimal note) {
        return note == null ? "sans note" : note.stripTrailingZeros().toPlainString() + "/20";
    }

    private static String decrireEntretien(Entretien e) {
        return e.getDateEntretien().format(FORMAT_ENTRETIEN)
                + (e.getLieu() == null || e.getLieu().isBlank() ? "" : " · " + e.getLieu());
    }

    private static HistoriqueCandidatureDTO historiqueVersDTO(HistoriqueCandidature h) {
        StatutCandidature ancien = h.getAncienStatut();
        StatutCandidature nouveau = h.getNouveauStatut();
        return new HistoriqueCandidatureDTO(h.getId(), h.getAction().name(), h.getAction().getLibelle(),
                ancien == null ? null : ancien.getId(), ancien == null ? null : ancien.getLibelle(),
                nouveau == null ? null : nouveau.getId(), nouveau == null ? null : nouveau.getLibelle(),
                h.getAuteur(), h.getDetails(), h.getDateAction());
    }

    private OffreDTO versDTO(OffreEmploi o) {
        return new OffreDTO(o.getId(), o.getTitre(), o.getDescription(), o.getDatePublication(),
                o.getStatutOffreEmploi() == null ? null : o.getStatutOffreEmploi().getLibelle(),
                candidatureRepo.countByOffreEmploiId(o.getId()),
                o.getDemande() == null ? null : o.getDemande().getIdDemande());
    }

    private CandidatureDTO versDTO(Candidature c) {
        Candidat p = c.getCandidat();
        Evaluation test = evaluationRepo.findByCandidatureId(c.getId()).orElse(null);
        CandidatureDTO.TestDTO testDTO = null;
        CandidatureDTO.EntretienDTO entretienDTO = null;
        if (test != null) {
            testDTO = new CandidatureDTO.TestDTO(test.getId(), test.getNoteTest(),
                    test.getStatutEvaluation() == null ? null : test.getStatutEvaluation().getId(),
                    test.getStatutEvaluation() == null ? null : test.getStatutEvaluation().getLibelle());
            entretienDTO = entretienRepo.findFirstByEvaluationIdOrderByIdDesc(test.getId())
                    .map(e -> new CandidatureDTO.EntretienDTO(e.getId(), e.getDateEntretien(), e.getLieu(),
                            e.getCommentaire(),
                            e.getStatutEntretien() == null ? null : e.getStatutEntretien().getId(),
                            e.getStatutEntretien() == null ? null : e.getStatutEntretien().getLibelle()))
                    .orElse(null);
        }
        String cvUrl = p.getFichierCvUrl() == null
                ? null
                : "/candidatures/" + c.getId() + "/cv";
        return new CandidatureDTO(c.getId(), c.getOffreEmploi().getId(), c.getOffreEmploi().getTitre(),
                p.getId(), p.getNom(), p.getPrenom(), p.getEmail(), p.getTelephone(), cvUrl,
                c.getDateCandidature(), c.getStatutCandidature().getId(), c.getStatutCandidature().getLibelle(),
                testDTO, entretienDTO);
    }
}
