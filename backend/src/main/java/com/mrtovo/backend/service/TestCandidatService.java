package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.QuestionnaireDTO;
import com.mrtovo.backend.dto.QuestionnaireRequest;
import com.mrtovo.backend.dto.ReponsesTestRequest;
import com.mrtovo.backend.dto.ResultatTestDTO;
import com.mrtovo.backend.dto.TestCandidatDTO;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.exception.RegleMetierException;
import com.mrtovo.backend.exception.RessourceIntrouvableException;
import com.mrtovo.backend.model.ActionCandidature;
import com.mrtovo.backend.model.Candidat;
import com.mrtovo.backend.model.Candidature;
import com.mrtovo.backend.model.ChoixQuestion;
import com.mrtovo.backend.model.Evaluation;
import com.mrtovo.backend.model.OffreEmploi;
import com.mrtovo.backend.model.QuestionTest;
import com.mrtovo.backend.model.ReponseCandidat;
import com.mrtovo.backend.model.Statuts;
import com.mrtovo.backend.repository.CandidatureRepository;
import com.mrtovo.backend.repository.EvaluationRepository;
import com.mrtovo.backend.repository.OffreEmploiRepository;
import com.mrtovo.backend.repository.QuestionTestRepository;
import com.mrtovo.backend.repository.ReponseCandidatRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Test en ligne du Bloc 2.
 * Le RH ecrit le QCM d'une offre ; le candidat retenu y repond une seule fois depuis son espace ;
 * la note sur 20 en decoule, et le RH valide ou refuse le test dans le kanban.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TestCandidatService {

    private static final BigDecimal VINGT = BigDecimal.valueOf(20);

    private final OffreEmploiRepository offreRepo;
    private final QuestionTestRepository questionRepo;
    private final ReponseCandidatRepository reponseRepo;
    private final CandidatureRepository candidatureRepo;
    private final EvaluationRepository evaluationRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final RecrutementService recrutementService;

    // ================================================================ RH : le questionnaire

    @Transactional(readOnly = true)
    public QuestionnaireDTO questionnaire(Long offreId) {
        OffreEmploi offre = chargerOffre(offreId);
        List<QuestionnaireDTO.Question> questions = questionRepo.findByOffreEmploiIdOrderByOrdreAsc(offreId).stream()
                .map(q -> new QuestionnaireDTO.Question(q.getId(), q.getEnonce(), q.getChoix().stream()
                        .map(ch -> new QuestionnaireDTO.Choix(ch.getId(), ch.getLibelle(), ch.isCorrect()))
                        .toList()))
                .toList();
        return new QuestionnaireDTO(offre.getId(), offre.getTitre(), reponseRepo.existsByQuestionOffreEmploiId(offreId),
                questions);
    }

    /** Remplace tout le questionnaire. Interdit des qu'un candidat a repondu : sa note ne correspondrait plus. */
    public QuestionnaireDTO enregistrerQuestionnaire(Long offreId, QuestionnaireRequest requete) {
        OffreEmploi offre = chargerOffre(offreId);
        if (reponseRepo.existsByQuestionOffreEmploiId(offreId)) {
            throw new RegleMetierException(
                    "Des candidats ont déjà répondu à ce questionnaire : il ne peut plus être modifié.");
        }
        for (int i = 0; i < requete.questions().size(); i++) {
            long bonnes = requete.questions().get(i).choix().stream().filter(QuestionnaireRequest.Choix::correct).count();
            if (bonnes != 1) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "La question " + (i + 1) + " doit avoir exactement une bonne réponse.");
            }
        }

        // Suppression puis flush avant de recreer : Hibernate executerait sinon les insertions en premier.
        questionRepo.deleteAll(questionRepo.findByOffreEmploiIdOrderByOrdreAsc(offreId));
        questionRepo.flush();

        int ordreQuestion = 1;
        for (QuestionnaireRequest.Question q : requete.questions()) {
            QuestionTest question = new QuestionTest();
            question.setOffreEmploi(offre);
            question.setOrdre(ordreQuestion++);
            question.setEnonce(q.enonce().trim());
            int ordreChoix = 1;
            for (QuestionnaireRequest.Choix c : q.choix()) {
                ChoixQuestion choix = new ChoixQuestion();
                choix.setQuestion(question);
                choix.setOrdre(ordreChoix++);
                choix.setLibelle(c.libelle().trim());
                choix.setCorrect(c.correct());
                question.getChoix().add(choix);
            }
            questionRepo.save(question);
        }
        questionRepo.flush();
        return questionnaire(offreId);
    }

    // ================================================================ Candidat : passer le test

    @Transactional(readOnly = true)
    public List<TestCandidatDTO.Resume> testsDuCandidat(String email) {
        Candidat candidat = candidatConnecte(email);
        return candidatureRepo.findByCandidatIdOrderByDateCandidatureDescIdDesc(candidat.getId()).stream()
                .map(c -> evaluationRepo.findByCandidatureId(c.getId())
                        .map(test -> new TestCandidatDTO.Resume(c.getId(), c.getOffreEmploi().getTitre(),
                                etat(c, test), (int) questionRepo.countByOffreEmploiId(c.getOffreEmploi().getId()),
                                test.getDateSoumission()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    /** Les questions sans leurs bonnes reponses : le DTO candidat n'a pas de champ pour les porter. */
    @Transactional(readOnly = true)
    public TestCandidatDTO.Questionnaire testAPasser(String email, Long candidatureId) {
        Candidature candidature = candidatureDuCandidat(email, candidatureId);
        Evaluation test = evaluationRepo.findByCandidatureId(candidatureId)
                .orElseThrow(() -> new RegleMetierException("Aucun test n'est prévu pour cette candidature."));
        verifierPassable(candidature, test);
        List<TestCandidatDTO.Question> questions = questionRepo
                .findByOffreEmploiIdOrderByOrdreAsc(candidature.getOffreEmploi().getId()).stream()
                .map(q -> new TestCandidatDTO.Question(q.getId(), q.getEnonce(), q.getChoix().stream()
                        .map(ch -> new TestCandidatDTO.Choix(ch.getId(), ch.getLibelle()))
                        .toList()))
                .toList();
        return new TestCandidatDTO.Questionnaire(candidatureId, candidature.getOffreEmploi().getTitre(), questions);
    }

    /** Une seule tentative : le verrou sur l'evaluation empeche deux envois simultanes. */
    public TestCandidatDTO.Resume soumettre(String email, Long candidatureId, ReponsesTestRequest requete) {
        Candidature candidature = candidatureDuCandidat(email, candidatureId);
        Evaluation test = evaluationRepo.verrouillerParCandidature(candidatureId)
                .orElseThrow(() -> new RegleMetierException("Aucun test n'est prévu pour cette candidature."));
        verifierPassable(candidature, test);

        List<QuestionTest> questions = questionRepo.findByOffreEmploiIdOrderByOrdreAsc(candidature.getOffreEmploi().getId());
        Map<Long, Long> choixParQuestion = new HashMap<>();
        for (ReponsesTestRequest.Reponse r : requete.reponses()) {
            if (choixParQuestion.put(r.questionId(), r.choixId()) != null) {
                throw new ResponseStatusException(BAD_REQUEST, "Une question a reçu plusieurs réponses.");
            }
        }
        if (choixParQuestion.size() != questions.size()
                || !questions.stream().allMatch(q -> choixParQuestion.containsKey(q.getId()))) {
            throw new ResponseStatusException(BAD_REQUEST, "Répondez à toutes les questions, une réponse par question.");
        }

        int bonnes = 0;
        for (QuestionTest question : questions) {
            Long choixId = choixParQuestion.get(question.getId());
            ChoixQuestion choix = question.getChoix().stream()
                    .filter(candidat -> candidat.getId().equals(choixId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST,
                            "Ce choix ne correspond pas à la question posée."));
            ReponseCandidat reponse = new ReponseCandidat();
            reponse.setEvaluation(test);
            reponse.setQuestion(question);
            reponse.setChoix(choix);
            reponseRepo.save(reponse);
            if (choix.isCorrect()) {
                bonnes++;
            }
        }

        BigDecimal note = calculerNote(bonnes, questions.size());
        test.setNoteTest(note);
        test.setDateSoumission(LocalDateTime.now());
        // Signee par le candidat : c'est lui qui a produit cette note, pas le RH.
        recrutementService.tracer(candidature, ActionCandidature.TEST_PASSE, null, null, email.toLowerCase(),
                RecrutementService.note(note) + " · " + bonnes + " bonne(s) réponse(s) sur " + questions.size());
        return new TestCandidatDTO.Resume(candidatureId, candidature.getOffreEmploi().getTitre(), "ENVOYE",
                questions.size(), test.getDateSoumission());
    }

    /** Bonnes reponses / nombre de questions x 20, arrondi au centieme. */
    static BigDecimal calculerNote(int bonnes, int total) {
        return VINGT.multiply(BigDecimal.valueOf(bonnes)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    // ================================================================ RH : le resultat

    @Transactional(readOnly = true)
    public ResultatTestDTO resultat(Long candidatureId) {
        Candidature candidature = candidatureRepo.findById(candidatureId)
                .orElseThrow(() -> new RessourceIntrouvableException("Candidature #" + candidatureId + " introuvable"));
        Evaluation test = evaluationRepo.findByCandidatureId(candidature.getId())
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun test pour la candidature #" + candidatureId));
        List<ResultatTestDTO.Ligne> lignes = reponseRepo.findByEvaluationIdOrderByQuestionOrdreAsc(test.getId()).stream()
                .map(r -> new ResultatTestDTO.Ligne(r.getQuestion().getEnonce(), r.getChoix().getLibelle(),
                        r.getQuestion().getChoix().stream().filter(ChoixQuestion::isCorrect)
                                .map(ChoixQuestion::getLibelle).findFirst().orElse("—"),
                        r.getChoix().isCorrect()))
                .toList();
        long bonnes = lignes.stream().filter(ResultatTestDTO.Ligne::correcte).count();
        return new ResultatTestDTO(candidatureId, test.getDateSoumission(), test.getNoteTest(), bonnes, lignes.size(),
                lignes);
    }

    // ================================================================ Outils

    private String etat(Candidature c, Evaluation test) {
        if (!c.getStatutCandidature().getId().equals(Statuts.Candidature.TEST)) {
            return "TERMINE";
        }
        if (test.getDateSoumission() != null) {
            return "ENVOYE";
        }
        if (test.getNoteTest() != null) {
            return "EVALUE";
        }
        return questionRepo.countByOffreEmploiId(c.getOffreEmploi().getId()) > 0 ? "A_PASSER" : "EN_PREPARATION";
    }

    private void verifierPassable(Candidature candidature, Evaluation test) {
        switch (etat(candidature, test)) {
            case "A_PASSER" -> { }
            case "ENVOYE" -> throw new RegleMetierException("Vous avez déjà envoyé vos réponses : une seule tentative est possible.");
            case "EN_PREPARATION" -> throw new RegleMetierException("Le test n'est pas encore prêt. Revenez plus tard.");
            case "EVALUE" -> throw new RegleMetierException("Ce test a déjà été évalué par le recruteur.");
            default -> throw new RegleMetierException("Le test de cette candidature est terminé.");
        }
    }

    private Candidat candidatConnecte(String email) {
        Utilisateur compte = utilisateurRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Compte introuvable."));
        if (compte.getCandidat() == null) {
            throw new ResponseStatusException(FORBIDDEN, "Ce compte n'est relié à aucun dossier de candidat.");
        }
        return compte.getCandidat();
    }

    /** 404 et non 403 : un candidat ne doit pas pouvoir deviner les candidatures des autres. */
    private Candidature candidatureDuCandidat(String email, Long candidatureId) {
        Candidat candidat = candidatConnecte(email);
        return candidatureRepo.findById(candidatureId)
                .filter(c -> c.getCandidat().getId().equals(candidat.getId()))
                .orElseThrow(() -> new RessourceIntrouvableException("Candidature #" + candidatureId + " introuvable"));
    }

    private OffreEmploi chargerOffre(Long offreId) {
        return offreRepo.findById(offreId)
                .orElseThrow(() -> new RessourceIntrouvableException("Offre #" + offreId + " introuvable"));
    }
}
