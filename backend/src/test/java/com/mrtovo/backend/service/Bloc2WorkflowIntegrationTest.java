package com.mrtovo.backend.service;

import com.mrtovo.backend.model.Statuts;
import com.mrtovo.backend.repository.CandidatRepository;
import com.mrtovo.backend.repository.CandidatureRepository;
import com.mrtovo.backend.repository.EvaluationRepository;
import com.mrtovo.backend.repository.OffreEmploiRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import com.mrtovo.backend.exception.RegleMetierException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Bloc2WorkflowIntegrationTest {

    private static final Path CV_DIRECTORY = Path.of(System.getProperty("java.io.tmpdir"),
            "rh-mrtovo-bloc2-tests-" + UUID.randomUUID()).toAbsolutePath();
    private final List<String> cvKeys = new ArrayList<>();

    @DynamicPropertySource
    static void configureCvStorage(DynamicPropertyRegistry registry) {
        registry.add("bloc2.cv-storage-dir", CV_DIRECTORY::toString);
    }

    @Autowired
    private RecrutementService recrutementService;
    @Autowired
    private OffreEmploiRepository offreRepository;
    @Autowired
    private CandidatRepository candidatRepository;
    @Autowired
    private CandidatureRepository candidatureRepository;
    @Autowired
    private EvaluationRepository evaluationRepository;
    @Autowired
    private TestCandidatService testService;
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Test
    void twoCandidatesAreSelectedTestedInterviewedAndDecidedIndependently() {
        Long offreId = recrutementService.listerOffresPubliques().stream()
                .findFirst().orElseThrow(() -> new AssertionError("Aucune offre publiée pour le scénario"))
                .id();
        var offre = offreRepository.findById(offreId).orElseThrow();
        var questionnaire = TestEnLigneSupport.questionnairePret(testService, offreId);
        String suffix = UUID.randomUUID().toString();
        var rejected = recrutementService.postuler(offreId, "Refus", "Test",
                "refus-" + suffix + "@example.test", null, cvPdf());
        var retained = recrutementService.postuler(offreId, "Retenu", "Test",
                "retenu-" + suffix + "@example.test", null, cvPdf());

        trackCv(rejected.email());
        trackCv(retained.email());
        assertTrue(candidatureRepository.countByOffreEmploiId(offreId) >= 2);

        assertEquals(Statuts.Candidature.REFUSE,
                recrutementService.preselectionner(rejected.id(), false).statutId());
        var candidatureTest = recrutementService.preselectionner(retained.id(), true);
        assertEquals(Statuts.Candidature.TEST, candidatureTest.statutId());
        assertNotNull(candidatureTest.test());

        assertThrows(RegleMetierException.class, () -> recrutementService.deciderTest(retained.id(), true));
        // Le candidat passe le test depuis son espace : la note vient de ses reponses.
        TestEnLigneSupport.compteCandidat(utilisateurRepository, candidatRepository, retained.email());
        int total = questionnaire.questions().size();
        testService.soumettre(retained.email(), retained.id(), TestEnLigneSupport.reponses(questionnaire, total));
        var note = TestCandidatService.calculerNote(total, total);
        assertEquals(Statuts.Candidature.ATTENTE_ENTRETIEN,
                recrutementService.deciderTest(retained.id(), true).statutId());

        var entretien = recrutementService.planifierEntretien(retained.id(),
                LocalDateTime.now().plusDays(2), "Salle de test");
        assertNotNull(entretien.entretien());
        assertEquals("Salle de test", entretien.entretien().lieu());

        assertEquals(Statuts.Candidature.ACCEPTE,
                recrutementService.resultatEntretien(entretien.entretien().id(), true,
                        "Entretien validé.").statutId());
        assertTrue(recrutementService.candidaturesAcceptees().stream()
                .anyMatch(candidature -> candidature.id().equals(retained.id())));
        assertEquals(note, evaluationRepository.findByCandidatureId(retained.id()).orElseThrow().getNoteTest());

        // Meme email sur la meme offre : refuse. Le retenu a desormais un compte : il doit s'y connecter.
        assertThrows(RegleMetierException.class, () -> recrutementService.postuler(offreId,
                "Autre nom", "Test", rejected.email(), null, cvPdf()));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> recrutementService.postuler(offreId, "Autre nom", "Test", retained.email(), null, cvPdf()));
        assertEquals(Statuts.OffreEmploi.OUVERTE,
                offre.getStatutOffreEmploi().getId());
    }

    @AfterAll
    void removeTestCvFiles() throws Exception {
        for (String key : cvKeys) {
            Files.deleteIfExists(CV_DIRECTORY.resolve(key).normalize());
        }
        if (Files.isDirectory(CV_DIRECTORY)) {
            try (var files = Files.list(CV_DIRECTORY)) {
                for (Path file : files.toList()) {
                    Files.deleteIfExists(file);
                }
            }
            Files.deleteIfExists(CV_DIRECTORY);
        }
    }

    private MockMultipartFile cvPdf() {
        byte[] bytes = "%PDF-1.7\nbloc2 integration test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return new MockMultipartFile("cv", "cv-test.pdf", "application/pdf", bytes);
    }

    private void trackCv(String email) {
        candidatRepository.findByEmailIgnoreCase(email)
                .map(com.mrtovo.backend.model.Candidat::getFichierCvUrl)
                .filter(key -> key != null)
                .ifPresent(cvKeys::add);
    }
}
