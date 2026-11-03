package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.ApiDtos.DemandeRecrutementRequest;
import com.mrtovo.backend.dto.ApiDtos.EstimationRequest;
import com.mrtovo.backend.dto.ApiDtos.LigneRequest;
import com.mrtovo.backend.dto.ApiDtos.ValidationRequest;
import com.mrtovo.backend.entity.*;
import com.mrtovo.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class Bloc1WorkflowIntegrationTest {

    @Autowired
    private DemandeService demandeService;
    @Autowired
    private RecrutementService recrutementService;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PosteRepository posteRepository;
    @Autowired
    private NiveauPosteRepository niveauRepository;
    @Autowired
    private GrilleService grilleService;

    @Test
    void managerThenRhFinanceAndDirectionApproveBeforeRhPublishesOffer() {
        Utilisateur manager = utilisateur(RoleUtilisateur.MANAGER);
        Utilisateur rh = utilisateur(RoleUtilisateur.RH);
        Utilisateur finance = utilisateur(RoleUtilisateur.FINANCE);
        Utilisateur direction = utilisateur(RoleUtilisateur.DIRECTION);
        Poste poste = posteRepository.findAll().stream().findFirst().orElseThrow();
        NiveauPoste niveau = niveauRepository.findAllByOrderByRangAsc().stream().findFirst().orElseThrow();
        GrilleSalariale grille = grilleService.applicable(poste, niveau, LocalDate.now()).orElseThrow();
        BigDecimal estimation = grille.getSalaireMin();

        var demande = demandeService.creer(new DemandeRecrutementRequest(
                poste.getIdPoste(), "CREATION", "Validation du workflow de recrutement", null,
                estimation.multiply(BigDecimal.valueOf(4)), LocalDate.now().plusDays(7),
                List.of(new LigneRequest(niveau.getIdNiveau(), 1))), manager.getEmail());

        assertEquals("EN_ATTENTE_RH", demande.statut());
        assertNotNull(demande.idOffre());
        assertEquals("En attente", demande.statutOffre());

        var ligne = demande.lignes().getFirst();
        demande = demandeService.valider(demande.idDemande(),
                new ValidationRequest("FAVORABLE", "Budget RH estimé.", List.of(
                        new EstimationRequest(ligne.idLigne(), estimation, null, null))), rh.getEmail());
        assertEquals("EN_ATTENTE_FINANCE", demande.statut());
        assertEquals("FAVORABLE", demande.validations().getFirst().decision());

        demande = demandeService.valider(demande.idDemande(),
                new ValidationRequest("FAVORABLE", "Budget disponible.", null), finance.getEmail());
        assertEquals("EN_ATTENTE_DIRECTION", demande.statut());

        demande = demandeService.valider(demande.idDemande(),
                new ValidationRequest("FAVORABLE", "Autorisation de recrutement.", null), direction.getEmail());
        assertEquals("AUTORISEE", demande.statut());
        assertEquals(3, demande.validations().size());

        Long offrePublieeId = demande.idOffre();
        demande = demandeService.ouvrir(demande.idDemande(), rh.getEmail());
        assertEquals("RECRUTEMENT_OUVERT", demande.statut());
        assertEquals("Ouverte", demande.statutOffre());
        assertTrue(recrutementService.listerOffresPubliques().stream()
                .anyMatch(offre -> offre.id().equals(offrePublieeId)));
    }

    private Utilisateur utilisateur(RoleUtilisateur role) {
        return utilisateurRepository.findAll().stream()
                .filter(utilisateur -> utilisateur.getRole() == role)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucun compte de test pour le rôle " + role));
    }
}
