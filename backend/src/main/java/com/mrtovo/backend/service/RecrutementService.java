package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.CandidatureDTO;
import com.mrtovo.backend.dto.OffreDTO;
import com.mrtovo.backend.exception.RegleMetierException;
import com.mrtovo.backend.exception.RessourceIntrouvableException;
import com.mrtovo.backend.model.*;
import com.mrtovo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Parcours d'une candidature :
 *   À traiter --présélection--> Test --décision test--> Attente entretien --résultat entretien--> Accepté
 *        \--rejet--> Refusé       \--échec--> Refusé                          \--refus--> Refusé
 * Une candidature "Accepté" est transmise à Sarobidy pour la suite (embauche).
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

    // ------------------------------------------------------------ 1. Présélection

    public CandidatureDTO preselectionner(Long candidatureId, boolean retenue) {
        Candidature c = chargerCandidature(candidatureId);
        verifierStatut(c, Statuts.Candidature.A_TRAITER, "être présélectionnée");

        if (retenue) {
            Evaluation test = new Evaluation();
            test.setCandidature(c);
            test.setStatutEvaluation(statutEvaluationRepo.getReferenceById(Statuts.Evaluation.EN_ATTENTE));
            evaluationRepo.save(test);
            changerStatut(c, Statuts.Candidature.TEST);
        } else {
            changerStatut(c, Statuts.Candidature.REFUSE);
        }
        return versDTO(c);
    }

    // ------------------------------------------------------------------- 2. Test

    /** Saisie (ou correction) de la note : ne fait pas avancer la candidature. */
    public CandidatureDTO saisirNote(Long candidatureId, BigDecimal note) {
        Candidature c = chargerCandidature(candidatureId);
        verifierStatut(c, Statuts.Candidature.TEST, "recevoir une note de test");
        Evaluation test = chargerTest(c);
        test.setNoteTest(note);
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
            changerStatut(c, Statuts.Candidature.ATTENTE_ENTRETIEN);
        } else {
            test.setStatutEvaluation(statutEvaluationRepo.getReferenceById(Statuts.Evaluation.ECHOUE));
            changerStatut(c, Statuts.Candidature.REFUSE);
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
        return versDTO(c);
    }

    public CandidatureDTO annulerEntretien(Long entretienId) {
        Entretien e = chargerEntretienPlanifie(entretienId);
        e.setStatutEntretien(statutEntretienRepo.getReferenceById(Statuts.Entretien.ANNULE));
        return versDTO(e.getEvaluation().getCandidature());
    }

    /** Résultat de l'entretien : accepté (→ Sarobidy) ou refusé. */
    public CandidatureDTO resultatEntretien(Long entretienId, boolean accepte, String commentaire) {
        Entretien e = chargerEntretienPlanifie(entretienId);
        Candidature c = e.getEvaluation().getCandidature();
        verifierStatut(c, Statuts.Candidature.ATTENTE_ENTRETIEN, "recevoir un résultat d'entretien");
        e.setCommentaire(commentaire);
        e.setStatutEntretien(statutEntretienRepo.getReferenceById(Statuts.Entretien.TERMINE));
        changerStatut(c, accepte ? Statuts.Candidature.ACCEPTE : Statuts.Candidature.REFUSE);
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

    private void changerStatut(Candidature c, long statutId) {
        c.setStatutCandidature(statutCandidatureRepo.findById(statutId).orElseThrow());
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
                    test.getStatutEvaluation().getId(), test.getStatutEvaluation().getLibelle());
            entretienDTO = entretienRepo.findFirstByEvaluationIdOrderByIdDesc(test.getId())
                    .map(e -> new CandidatureDTO.EntretienDTO(e.getId(), e.getDateEntretien(), e.getLieu(),
                            e.getCommentaire(), e.getStatutEntretien().getId(), e.getStatutEntretien().getLibelle()))
                    .orElse(null);
        }
        return new CandidatureDTO(c.getId(), c.getOffreEmploi().getId(), c.getOffreEmploi().getTitre(),
                p.getId(), p.getNom(), p.getPrenom(), p.getEmail(), p.getTelephone(), p.getFichierCvUrl(),
                c.getDateCandidature(), c.getStatutCandidature().getId(), c.getStatutCandidature().getLibelle(),
                testDTO, entretienDTO);
    }
}
