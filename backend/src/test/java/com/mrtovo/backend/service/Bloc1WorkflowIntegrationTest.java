package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.ApiDtos.DemandeRecrutementRequest;
import com.mrtovo.backend.dto.ApiDtos.LigneRequest;
import com.mrtovo.backend.dto.ApiDtos.ValidationRequest;
import com.mrtovo.backend.entity.*;
import com.mrtovo.backend.exception.BusinessException;
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

    @Test
    void directionAloneAcceptsRecruitmentRequestBeforeRhPublishesOffer() {
        Utilisateur manager = utilisateur(RoleUtilisateur.MANAGER);
        Utilisateur rh = utilisateur(RoleUtilisateur.RH);
        Utilisateur finance = utilisateur(RoleUtilisateur.FINANCE);
        Utilisateur direction = utilisateur(RoleUtilisateur.DIRECTION);
        Poste poste = posteRepository.findAll().stream().findFirst().orElseThrow();
        NiveauPoste niveau = niveauRepository.findAllByOrderByRangAsc().stream().findFirst().orElseThrow();

        var demande = demandeService.creer(new DemandeRecrutementRequest(
                poste.getIdPoste(), "CREATION", "Validation du workflow de recrutement", null,
                BigDecimal.valueOf(4_000_000), LocalDate.now().plusDays(7),
                List.of(new LigneRequest(niveau.getIdNiveau(), 1))), manager.getEmail());

        assertEquals("EN_ATTENTE_DIRECTION", demande.statut());
        assertNotNull(demande.idOffre());
        assertEquals("En attente", demande.statutOffre());

        Long idDemande = demande.idDemande();
        ValidationRequest favorable = new ValidationRequest("FAVORABLE", "Demande conforme.", null);
        assertThrows(BusinessException.class,
                () -> demandeService.valider(idDemande, favorable, rh.getEmail()));
        assertThrows(BusinessException.class,
                () -> demandeService.valider(idDemande, favorable, finance.getEmail()));
        assertEquals("EN_ATTENTE_DIRECTION", demandeService.detail(idDemande, direction.getEmail()).statut());

        demande = demandeService.valider(idDemande, favorable, direction.getEmail());
        assertEquals("AUTORISEE", demande.statut());
        assertEquals(1, demande.validations().size());
        assertEquals("DIRECTION", demande.validations().getFirst().etape());
        assertThrows(BusinessException.class,
                () -> demandeService.valider(idDemande, favorable, direction.getEmail()));

        Long offrePublieeId = demande.idOffre();
        demande = demandeService.ouvrir(idDemande, rh.getEmail());
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
