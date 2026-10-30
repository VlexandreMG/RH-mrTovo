package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.Affectation;
import com.mrtovo.backend.entity.CandidatRecrutement;
import com.mrtovo.backend.entity.CandidatureSelection;
import com.mrtovo.backend.entity.Contrat;
import com.mrtovo.backend.entity.DepartementRecrutement;
import com.mrtovo.backend.entity.GrilleSalarialeRecrutement;
import com.mrtovo.backend.entity.OffreRecrutement;
import com.mrtovo.backend.entity.PosteRecrutement;
import com.mrtovo.backend.entity.StatutCandidatureRecrutement;
import com.mrtovo.backend.entity.StatutOffreRecrutement;
import com.mrtovo.backend.entity.StatutProposition;
import com.mrtovo.backend.repository.AffectationRepository;
import com.mrtovo.backend.repository.CandidatRecrutementRepository;
import com.mrtovo.backend.repository.CandidatureSelectionRepository;
import com.mrtovo.backend.repository.ContratRepository;
import com.mrtovo.backend.repository.DepartementRecrutementRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.GrilleRecrutementRepository;
import com.mrtovo.backend.repository.OffreRecrutementRepository;
import com.mrtovo.backend.repository.PosteRecrutementRepository;
import com.mrtovo.backend.dto.AcceptionRequest;
import com.mrtovo.backend.dto.EmissionRequest;
import com.mrtovo.backend.dto.PropositionCreateRequest;
import com.mrtovo.backend.dto.ValidationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class RecrutementDatabaseWorkflowIntegrationTest {

    @Autowired
    private PropositionService propositionService;
    @Autowired
    private DepartementRecrutementRepository departementRepository;
    @Autowired
    private PosteRecrutementRepository posteRepository;
    @Autowired
    private GrilleRecrutementRepository grilleRepository;
    @Autowired
    private CandidatRecrutementRepository candidatRepository;
    @Autowired
    private OffreRecrutementRepository offreRepository;
    @Autowired
    private CandidatureSelectionRepository candidatureRepository;
    @Autowired
    private EmployeEmbaucheRepository employeRepository;
    @Autowired
    private ContratRepository contratRepository;
    @Autowired
    private AffectationRepository affectationRepository;

    @Test
    void createsOfferAndHiringRecordsWithExistingRecrutementTables() {
        String suffix = UUID.randomUUID().toString();
        DepartementRecrutement departement = new DepartementRecrutement();
        departement.setNom("Test Bloc 3 " + suffix);
        departement.setCode("T3-" + suffix.substring(0, 8));
        departement = departementRepository.saveAndFlush(departement);

        PosteRecrutement poste = new PosteRecrutement();
        poste.setTitre("PosteRecrutement de test Bloc 3 " + suffix);
        poste.setDescription("Vérification transactionnelle des tables Bloc 3.");
        poste.setDepartement(departement);
        poste = posteRepository.saveAndFlush(poste);

        GrilleSalarialeRecrutement grille = new GrilleSalarialeRecrutement();
        grille.setPoste(poste);
        grille.setNiveau("TEST");
        grille.setSalaireMin(new BigDecimal("1000.00"));
        grille.setSalaireMax(new BigDecimal("5000.00"));
        grille.setDateDebut(LocalDate.now().minusDays(1));
        grilleRepository.saveAndFlush(grille);

        CandidatRecrutement candidat = new CandidatRecrutement();
        candidat.setNom("Test");
        candidat.setPrenom("Recrutement");
        candidat.setEmail("bloc3-" + suffix + "@example.test");
        candidat = candidatRepository.saveAndFlush(candidat);

        OffreRecrutement offre = new OffreRecrutement();
        offre.setTitre(poste.getTitre());
        offre.setDescription("Offre de test transactionnelle.");
        offre.setDatePublication(LocalDate.now());
        offre.setStatut(StatutOffreRecrutement.PUBLIEE);
        offre = offreRepository.saveAndFlush(offre);

        CandidatureSelection candidature = new CandidatureSelection();
        candidature.setCandidat(candidat);
        candidature.setOffreEmploi(offre);
        candidature.setDateCandidature(LocalDate.now());
        candidature.setStatut(StatutCandidatureRecrutement.RETENU);
        candidature = candidatureRepository.saveAndFlush(candidature);

        var proposition = propositionService.creer(new PropositionCreateRequest(
                candidature.getId(), poste.getId(), departement.getId(),
                new BigDecimal("3000.00"), "Temps plein", null, "Antananarivo",
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(5), null,
                null, null, null, "Test intégration base Bloc 3"));
        assertEquals(StatutProposition.BROUILLON, proposition.statut());

        propositionService.valider(proposition.id(), new ValidationRequest(
                null, "Dans la grille.", false, null));
        propositionService.emettre(proposition.id(), new EmissionRequest(
                null, LocalDate.now().plusDays(5), "Proposition émise pour test."));
        var acceptee = propositionService.accepter(proposition.id(), new AcceptionRequest(
                null, "Acceptation pour test.", LocalDate.now()));

        assertEquals(StatutProposition.ACCEPTEE, acceptee.statut());
        var employe = employeRepository.findByPropositionId(proposition.id()).orElseThrow();
        assertNotNull(employe.getMatricule());
        assertEquals(candidat.getId(), employe.getCandidat().getId());
        assertTrue(contratRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId()).stream()
                .map(Contrat::getReference)
                .anyMatch(reference -> reference != null && !reference.isBlank()));
        assertEquals(1, affectationRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId()).size());
    }
}
