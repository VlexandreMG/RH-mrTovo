package com.mrtovo.backend.service;

import tools.jackson.databind.ObjectMapper;
import com.mrtovo.backend.dto.QuestionnaireDTO;
import com.mrtovo.backend.dto.QuestionnaireRequest;
import com.mrtovo.backend.dto.ReponsesTestRequest;
import com.mrtovo.backend.dto.ResultatTestDTO;
import com.mrtovo.backend.exception.RegleMetierException;
import com.mrtovo.backend.exception.RessourceIntrouvableException;
import com.mrtovo.backend.model.OffreEmploi;
import com.mrtovo.backend.model.Statuts;
import com.mrtovo.backend.repository.CandidatRepository;
import com.mrtovo.backend.repository.EvaluationRepository;
import com.mrtovo.backend.repository.OffreEmploiRepository;
import com.mrtovo.backend.repository.StatutOffreEmploiRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test en ligne : le RH ecrit le QCM, le candidat y repond une fois, la note en decoule. */
@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestCandidatIntegrationTest {

    private static final Path CV_DIRECTORY = Path.of(System.getProperty("java.io.tmpdir"),
            "rh-mrtovo-test-en-ligne-" + UUID.randomUUID()).toAbsolutePath();

    @DynamicPropertySource
    static void configureCvStorage(DynamicPropertyRegistry registry) {
        registry.add("bloc2.cv-storage-dir", CV_DIRECTORY::toString);
    }

    @Autowired
    private RecrutementService recrutement;
    @Autowired
    private TestCandidatService tests;
    @Autowired
    private OffreEmploiRepository offres;
    @Autowired
    private StatutOffreEmploiRepository statutsOffre;
    @Autowired
    private EvaluationRepository evaluations;
    @Autowired
    private UtilisateurRepository utilisateurs;
    @Autowired
    private CandidatRepository candidats;
    @Autowired
    private ObjectMapper json;

    private OffreEmploi offre;
    private QuestionnaireDTO questionnaire;

    /** Une offre neuve par test : son QCM n'est verrouille par aucune reponse precedente. */
    @BeforeEach
    void preparer() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "rh@rh.com", null, List.of(new SimpleGrantedAuthority("ROLE_RH"))));
        offre = nouvelleOffre();
        questionnaire = tests.enregistrerQuestionnaire(offre.getId(), new QuestionnaireRequest(List.of(
                TestEnLigneSupport.question("Q1", "bonne 1", "fausse 1"),
                TestEnLigneSupport.question("Q2", "bonne 2", "fausse 2", "autre 2"),
                TestEnLigneSupport.question("Q3", "bonne 3", "fausse 3"))));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void leCandidatPasseLeTestEtLaNoteEstCalculeeParLeServeur() throws Exception {
        String email = candidatRetenu();

        assertEquals("A_PASSER", tests.testsDuCandidat(email).get(0).etat());
        var aPasser = tests.testAPasser(email, candidature(email));
        assertEquals(3, aPasser.questions().size());
        assertFalse(json.writeValueAsString(aPasser).contains("correct"),
                "les bonnes réponses ne doivent jamais partir vers le candidat");

        var resume = tests.soumettre(email, candidature(email), TestEnLigneSupport.reponses(questionnaire, 2));

        assertEquals("ENVOYE", resume.etat());
        var test = evaluations.findByCandidatureId(candidature(email)).orElseThrow();
        assertEquals(new BigDecimal("13.33"), test.getNoteTest());
        assertNotNull(test.getDateSoumission());

        ResultatTestDTO resultat = tests.resultat(candidature(email));
        assertEquals(2, resultat.nbBonnes());
        assertEquals(3, resultat.nbQuestions());
        assertEquals(List.of(true, true, false), resultat.reponses().stream().map(ResultatTestDTO.Ligne::correcte).toList());
        assertEquals("fausse 3", resultat.reponses().get(2).reponseDonnee());
        assertEquals("bonne 3", resultat.reponses().get(2).bonneReponse());

        var carte = recrutement.getCandidature(candidature(email));
        assertEquals(2, carte.test().nbBonnes());
        assertEquals(3, carte.test().nbQuestions());

        var ligne = recrutement.historique(candidature(email)).stream()
                .filter(h -> h.action().equals("TEST_PASSE")).findFirst().orElseThrow();
        assertEquals(email, ligne.auteur());
        assertEquals("13.33/20 · 2 bonne(s) réponse(s) sur 3", ligne.details());

        assertEquals(Statuts.Candidature.ATTENTE_ENTRETIEN,
                recrutement.deciderTest(candidature(email), true).statutId());
        assertEquals("REUSSI", tests.testsDuCandidat(email).get(0).etat());
    }

    @Test
    void unCandidatRefuseAuTestLeVoitDansSonEspace() {
        String email = candidatRetenu();
        tests.soumettre(email, candidature(email), TestEnLigneSupport.reponses(questionnaire, 0));

        assertEquals(Statuts.Candidature.REFUSE, recrutement.deciderTest(candidature(email), false).statutId());

        assertEquals("REFUSE", tests.testsDuCandidat(email).get(0).etat());
        assertThrows(RegleMetierException.class, () -> tests.testAPasser(email, candidature(email)));
    }

    @Test
    void unRejetALaPreselectionNeMontreAucunTest() {
        String email = deposer();
        TestEnLigneSupport.compteCandidat(utilisateurs, candidats, email);
        recrutement.preselectionner(candidature(email), false);

        assertTrue(tests.testsDuCandidat(email).isEmpty(), "aucun test n'a été proposé : rien à afficher");
    }

    @Test
    void uneSeuleTentative() {
        String email = candidatRetenu();
        tests.soumettre(email, candidature(email), TestEnLigneSupport.reponses(questionnaire, 3));

        assertThrows(RegleMetierException.class,
                () -> tests.soumettre(email, candidature(email), TestEnLigneSupport.reponses(questionnaire, 0)));
        assertThrows(RegleMetierException.class, () -> tests.testAPasser(email, candidature(email)));
        assertEquals(new BigDecimal("20.00"), evaluations.findByCandidatureId(candidature(email)).orElseThrow().getNoteTest());
    }

    @Test
    void leQuestionnaireSeVerrouilleDesQuUnCandidatARepondu() {
        String email = candidatRetenu();
        assertFalse(tests.questionnaire(offre.getId()).verrouille());
        tests.soumettre(email, candidature(email), TestEnLigneSupport.reponses(questionnaire, 1));

        assertTrue(tests.questionnaire(offre.getId()).verrouille());
        assertThrows(RegleMetierException.class, () -> tests.enregistrerQuestionnaire(offre.getId(),
                new QuestionnaireRequest(List.of(TestEnLigneSupport.question("Nouvelle", "a", "b")))));
    }

    @Test
    void desReponsesIncompletesOuEtrangeresSontRefuseesSansRienEnregistrer() {
        String email = candidatRetenu();
        var completes = TestEnLigneSupport.reponses(questionnaire, 3).reponses();

        var incompletes = new ReponsesTestRequest(completes.subList(0, 2));
        assertThrows(ResponseStatusException.class, () -> tests.soumettre(email, candidature(email), incompletes));

        List<ReponsesTestRequest.Reponse> melangees = new ArrayList<>(completes);
        melangees.set(0, new ReponsesTestRequest.Reponse(completes.get(0).questionId(), completes.get(1).choixId()));
        assertThrows(ResponseStatusException.class,
                () -> tests.soumettre(email, candidature(email), new ReponsesTestRequest(melangees)));

        List<ReponsesTestRequest.Reponse> doublon = new ArrayList<>(completes);
        doublon.add(completes.get(0));
        assertThrows(ResponseStatusException.class,
                () -> tests.soumettre(email, candidature(email), new ReponsesTestRequest(doublon)));

        assertNull(evaluations.findByCandidatureId(candidature(email)).orElseThrow().getNoteTest());
    }

    @Test
    void unCandidatNeVoitPasLeTestDUnAutre() {
        String email = candidatRetenu();
        String autre = deposer();
        TestEnLigneSupport.compteCandidat(utilisateurs, candidats, autre);

        assertThrows(RessourceIntrouvableException.class, () -> tests.testAPasser(autre, candidature(email)));
        assertThrows(RessourceIntrouvableException.class,
                () -> tests.soumettre(autre, candidature(email), TestEnLigneSupport.reponses(questionnaire, 3)));
    }

    @Test
    void onNeRetientPasUnCandidatTantQueLOffreNAPasDeQuestionnaire() {
        OffreEmploi sansQcm = nouvelleOffre();
        Long id = recrutement.postuler(sansQcm.getId(), "Sans", "Qcm", "sans-qcm-" + UUID.randomUUID() + "@example.test",
                null, cv()).id();

        assertThrows(RegleMetierException.class, () -> recrutement.preselectionner(id, true));
        assertEquals(Statuts.Candidature.REFUSE, recrutement.preselectionner(id, false).statutId());
    }

    @Test
    void onNeDecidePasDuTestAvantQueLeCandidatLAitPasse() {
        String email = candidatRetenu();

        assertThrows(RegleMetierException.class, () -> recrutement.deciderTest(candidature(email), true));
        assertEquals("A_PASSER", tests.testsDuCandidat(email).get(0).etat());
    }

    @Test
    void chaqueQuestionDoitAvoirExactementUneBonneReponse() {
        var deuxBonnes = new QuestionnaireRequest(List.of(new QuestionnaireRequest.Question("Q",
                List.of(new QuestionnaireRequest.Choix("a", true), new QuestionnaireRequest.Choix("b", true)))));
        var aucune = new QuestionnaireRequest(List.of(new QuestionnaireRequest.Question("Q",
                List.of(new QuestionnaireRequest.Choix("a", false), new QuestionnaireRequest.Choix("b", false)))));

        assertThrows(ResponseStatusException.class, () -> tests.enregistrerQuestionnaire(offre.getId(), deuxBonnes));
        assertThrows(ResponseStatusException.class, () -> tests.enregistrerQuestionnaire(offre.getId(), aucune));
        assertEquals(3, tests.questionnaire(offre.getId()).questions().size(), "l'ancien questionnaire reste intact");
    }

    @Test
    void enregistrerRemplaceToutLeQuestionnaire() {
        var nouveau = tests.enregistrerQuestionnaire(offre.getId(), new QuestionnaireRequest(List.of(
                TestEnLigneSupport.question("Seule question", "oui", "non"))));

        assertEquals(1, nouveau.questions().size());
        assertEquals("Seule question", nouveau.questions().get(0).enonce());
        assertTrue(nouveau.questions().get(0).choix().get(0).correct());
    }

    @Test
    void laNoteEstLaProportionDeBonnesReponsesSurVingt() {
        assertEquals(new BigDecimal("13.33"), TestCandidatService.calculerNote(2, 3));
        assertEquals(new BigDecimal("0.00"), TestCandidatService.calculerNote(0, 5));
        assertEquals(new BigDecimal("20.00"), TestCandidatService.calculerNote(5, 5));
        assertEquals(new BigDecimal("6.67"), TestCandidatService.calculerNote(1, 3));
    }

    @AfterAll
    void supprimerLesCv() throws Exception {
        if (Files.isDirectory(CV_DIRECTORY)) {
            try (var fichiers = Files.list(CV_DIRECTORY)) {
                for (Path fichier : fichiers.toList()) {
                    Files.deleteIfExists(fichier);
                }
            }
            Files.deleteIfExists(CV_DIRECTORY);
        }
    }

    // ================================================================ Outils

    private final java.util.Map<String, Long> candidatures = new java.util.HashMap<>();

    /** Un candidat qui a postule, a cree son compte, et que le RH a retenu. */
    private String candidatRetenu() {
        String email = deposer();
        TestEnLigneSupport.compteCandidat(utilisateurs, candidats, email);
        recrutement.preselectionner(candidature(email), true);
        return email;
    }

    private String deposer() {
        String email = "test-en-ligne-" + UUID.randomUUID() + "@example.test";
        Long id = recrutement.postuler(offre.getId(), "Andria", "Luc", email, null, cv()).id();
        candidatures.put(email, id);
        return email;
    }

    private Long candidature(String email) {
        return candidatures.get(email);
    }

    private OffreEmploi nouvelleOffre() {
        OffreEmploi o = new OffreEmploi();
        o.setTitre("Offre de test en ligne");
        o.setDescription("Offre créée pour les tests");
        o.setDatePublication(LocalDate.now());
        o.setStatutOffreEmploi(statutsOffre.getReferenceById(Statuts.OffreEmploi.OUVERTE));
        return offres.save(o);
    }

    private static MockMultipartFile cv() {
        return new MockMultipartFile("cv", "cv.pdf", "application/pdf",
                "%PDF-1.7\ntest en ligne".getBytes(StandardCharsets.US_ASCII));
    }
}
