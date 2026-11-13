package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.HistoriqueCandidatureDTO;
import com.mrtovo.backend.model.Statuts;
import com.mrtovo.backend.repository.CandidatRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Traçabilité du Bloc 2 : chaque action sur une candidature laisse une ligne datée, avec son auteur. */
@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Bloc2HistoriqueIntegrationTest {

    private static final Path CV_DIRECTORY = Path.of(System.getProperty("java.io.tmpdir"),
            "rh-mrtovo-historique-tests-" + UUID.randomUUID()).toAbsolutePath();

    @DynamicPropertySource
    static void configureCvStorage(DynamicPropertyRegistry registry) {
        registry.add("bloc2.cv-storage-dir", CV_DIRECTORY::toString);
    }

    @Autowired
    private RecrutementService service;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TestCandidatService testService;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private CandidatRepository candidatRepository;

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void leParcoursCompletEstTraceAvecAuteursEtStatuts() {
        Long candidatureId = deposer();
        var questionnaire = TestEnLigneSupport.questionnairePret(testService, offreId());
        int total = questionnaire.questions().size();
        connecter("rh@rh.com");

        service.preselectionner(candidatureId, true);
        String email = service.getCandidature(candidatureId).email();
        TestEnLigneSupport.compteCandidat(utilisateurRepository, candidatRepository, email);
        testService.soumettre(email, candidatureId, TestEnLigneSupport.reponses(questionnaire, total - 1));
        service.deciderTest(candidatureId, true);
        var planifie = service.planifierEntretien(candidatureId, LocalDateTime.of(2030, 3, 4, 10, 0), "Salle 2");
        service.annulerEntretien(planifie.entretien().id());
        var replanifie = service.planifierEntretien(candidatureId, LocalDateTime.of(2030, 3, 5, 14, 30), null);
        service.resultatEntretien(replanifie.entretien().id(), true, "  Très bon profil  ");

        List<HistoriqueCandidatureDTO> historique = service.historique(candidatureId);

        assertEquals(List.of("CANDIDATURE_DEPOSEE", "PRESELECTION_RETENUE", "TEST_PASSE",
                        "TEST_VALIDE", "ENTRETIEN_PLANIFIE", "ENTRETIEN_ANNULE", "ENTRETIEN_PLANIFIE",
                        "CANDIDAT_ACCEPTE"),
                historique.stream().map(HistoriqueCandidatureDTO::action).toList());

        HistoriqueCandidatureDTO depot = historique.get(0);
        assertTrue(depot.auteur().startsWith("historique-"), "le dépôt public est signé par le candidat");
        assertNull(depot.ancienStatutId());
        assertEquals(Statuts.Candidature.A_TRAITER, depot.nouveauStatutId());

        String note = RecrutementService.note(TestCandidatService.calculerNote(total - 1, total));
        HistoriqueCandidatureDTO testPasse = historique.get(2);
        assertEquals(email, testPasse.auteur(), "la note est produite par le candidat, pas par le RH");
        assertNull(testPasse.nouveauStatutId(), "passer le test ne change pas le statut");
        assertEquals(note + " · " + (total - 1) + " bonne(s) réponse(s) sur " + total, testPasse.details());

        assertEquals(Statuts.Candidature.A_TRAITER, historique.get(1).ancienStatutId());
        assertEquals(Statuts.Candidature.TEST, historique.get(1).nouveauStatutId());
        for (int i : List.of(1, 3, 4, 5, 6, 7)) {
            assertEquals("rh@rh.com", historique.get(i).auteur());
        }
        assertEquals(note, historique.get(3).details());
        assertEquals("04/03/2030 à 10:00 · Salle 2", historique.get(4).details());
        assertEquals("04/03/2030 à 10:00 · Salle 2", historique.get(5).details());
        assertEquals("05/03/2030 à 14:30", historique.get(6).details());
        assertEquals(Statuts.Candidature.ACCEPTE, historique.get(7).nouveauStatutId());
        assertEquals("Très bon profil", historique.get(7).details());
    }

    @Test
    void unRejetALaPresélectionEstTrace() {
        Long candidatureId = deposer();
        connecter("rh@rh.com");
        service.preselectionner(candidatureId, false);

        HistoriqueCandidatureDTO rejet = service.historique(candidatureId).get(1);
        assertEquals("PRESELECTION_REJETEE", rejet.action());
        assertEquals(Statuts.Candidature.REFUSE, rejet.nouveauStatutId());
    }

    @Test
    void uneLigneDHistoriqueNePeutPasEtreModifiee() {
        Long candidatureId = deposer();
        entityManager.flush();

        int lignes = jdbc.update("UPDATE mrtovo.historique_candidature SET auteur = 'falsifié' WHERE candidature_id = ?",
                candidatureId);

        assertEquals(0, lignes);
        assertTrue(service.historique(candidatureId).get(0).auteur().startsWith("historique-"));
    }

    @Test
    void lesCandidaturesAnterieuresOntLeurDepotRepris() {
        Integer sansHistorique = jdbc.queryForObject("""
                SELECT COUNT(*) FROM mrtovo.candidature c
                WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature h WHERE h.candidature_id = c.id)
                """, Integer.class);

        assertEquals(0, sansHistorique);
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

    private Long offreId() {
        return service.listerOffresPubliques().stream().findFirst()
                .orElseThrow(() -> new AssertionError("Aucune offre publiée pour le scénario")).id();
    }

    private Long deposer() {
        Long offreId = offreId();
        String email = "historique-" + UUID.randomUUID() + "@example.test";
        byte[] pdf = "%PDF-1.7\nhistorique".getBytes(StandardCharsets.US_ASCII);
        return service.postuler(offreId, "Rabe", "Test", email, null,
                new MockMultipartFile("cv", "cv.pdf", "application/pdf", pdf)).id();
    }

    private static void connecter(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_RH"))));
    }
}
