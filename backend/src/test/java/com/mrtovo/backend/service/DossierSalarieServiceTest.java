package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.DossierSalarieDto;
import com.mrtovo.backend.dto.EvenementCarriereDto;
import com.mrtovo.backend.dto.IdentiteRequest;
import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.Contrat;
import com.mrtovo.backend.entity.DocumentSalarie;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.EtatValidite;
import com.mrtovo.backend.entity.JournalAudit;
import com.mrtovo.backend.entity.StatutContrat;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.TypeContrat;
import com.mrtovo.backend.entity.TypeDocument;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.AffectationRepository;
import com.mrtovo.backend.repository.ContratRepository;
import com.mrtovo.backend.repository.DocumentSalarieRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.JournalAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DossierSalarieServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 3);

    private EmployeEmbaucheRepository employeRepository;
    private ContratRepository contratRepository;
    private AffectationRepository affectationRepository;
    private DocumentSalarieRepository documentRepository;
    private JournalAuditRepository journalRepository;
    private DossierSalarieService service;
    private EmployeEmbauche employe;

    @TempDir
    Path dossierTemporaire;

    @BeforeEach
    void preparer() {
        employeRepository = mock(EmployeEmbaucheRepository.class);
        contratRepository = mock(ContratRepository.class);
        affectationRepository = mock(AffectationRepository.class);
        documentRepository = mock(DocumentSalarieRepository.class);
        journalRepository = mock(JournalAuditRepository.class);
        Clock horloge = Clock.fixed(AUJOURDHUI.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        service = new DossierSalarieService(employeRepository, contratRepository, affectationRepository,
                documentRepository, journalRepository,
                new DocumentStorageService(dossierTemporaire.toString()),
                mock(RecrutementMapper.class), horloge);

        employe = new EmployeEmbauche();
        employe.setId(1L);
        employe.setMatricule("EMP-2026-0001");
        employe.setNom("Rakoto");
        employe.setPrenom("Jean");
        employe.setEmail("jean.rakoto@mail.com");
        employe.setDateEmbauche(LocalDate.of(2025, 1, 10));
        employe.setDateEntree(LocalDate.of(2025, 2, 1));
        employe.setStatut(StatutEmploye.ACTIF);
        when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(contratRepository.findByEmployeIdOrderByDateDebutAsc(1L)).thenReturn(List.of());
        when(affectationRepository.findByEmployeIdOrderByDateDebutAsc(1L)).thenReturn(List.of());
        when(documentRepository.findByEmployeIdOrderByDateDepotDesc(1L)).thenReturn(List.of());
    }

    @Test
    void calculeLEtatDeValiditeDUnDocument() {
        assertEquals(EtatValidite.PERMANENT, EtatValidite.calculer(null, AUJOURDHUI));
        assertEquals(EtatValidite.EXPIRE, EtatValidite.calculer(AUJOURDHUI.minusDays(1), AUJOURDHUI));
        assertEquals(EtatValidite.EXPIRE_BIENTOT, EtatValidite.calculer(AUJOURDHUI, AUJOURDHUI));
        assertEquals(EtatValidite.EXPIRE_BIENTOT, EtatValidite.calculer(AUJOURDHUI.plusDays(30), AUJOURDHUI));
        assertEquals(EtatValidite.VALIDE, EtatValidite.calculer(AUJOURDHUI.plusDays(31), AUJOURDHUI));
    }

    @Test
    void consulterUnDossierEcritUneLigneDAudit() {
        DossierSalarieDto dossier = service.dossier(1L, "rh@rh.com");

        ArgumentCaptor<JournalAudit> trace = ArgumentCaptor.forClass(JournalAudit.class);
        verify(journalRepository).save(trace.capture());
        assertEquals(ActionAudit.CONSULTATION_DOSSIER, trace.getValue().getAction());
        assertEquals("rh@rh.com", trace.getValue().getAuteur());
        assertEquals("1 an 9 mois", dossier.situation().anciennete());
    }

    @Test
    void laCarriereFaitApparaitreLEvolutionDeSalaireEtDeContrat() {
        Contrat essai = contrat("CTR-2026-0001", TypeContrat.PERIODE_DESSAI, LocalDate.of(2025, 2, 1), "1000000",
                StatutContrat.TERMINE);
        essai.setDateFin(LocalDate.of(2025, 8, 1));
        Contrat cdi = contrat("CTR-2026-0002", TypeContrat.CDI, LocalDate.of(2025, 8, 1), "1200000", StatutContrat.ACTIF);

        List<EvenementCarriereDto> carriere = service.construireCarriere(employe, List.of(essai, cdi), List.of());

        assertTrue(carriere.stream().anyMatch(e -> e.type().equals("EVOLUTION_SALAIRE")
                && e.description().contains("+20.0 %")));
        assertTrue(carriere.stream().anyMatch(e -> e.type().equals("CHANGEMENT_CONTRAT")));
        assertEquals("EMBAUCHE", carriere.get(0).type());
        assertEquals(cdi, DossierSalarieService.contratEnCours(List.of(essai, cdi), AUJOURDHUI));
    }

    @Test
    void signaleLesDocumentsExpiresEtLesPiecesManquantes() {
        DocumentSalarie cin = new DocumentSalarie();
        cin.setEmploye(employe);
        cin.setTypeDocument(TypeDocument.CIN);
        cin.setLibelle("CIN");
        cin.setDateExpiration(AUJOURDHUI.minusDays(3));

        List<String> alertes = service.construireAlertes(employe, List.of(), List.of(cin), AUJOURDHUI);

        assertTrue(alertes.stream().anyMatch(a -> a.startsWith("CIN expiré")));
        // Une CIN expiree ne compte pas comme presente.
        assertTrue(alertes.contains("Pièce obligatoire manquante : Carte d'identité nationale"));
        assertTrue(alertes.contains("Pièce obligatoire manquante : RIB".replace("RIB", "Relevé d'identité bancaire")));
    }

    @Test
    void modifierLIdentiteTraceChaqueChampChange() {
        when(employeRepository.findByEmailIgnoreCase("jean.rakoto@mail.com")).thenReturn(Optional.of(employe));

        service.modifierIdentite(1L, identite("Rakoto", "0341234567"), "rh@rh.com");

        ArgumentCaptor<JournalAudit> trace = ArgumentCaptor.forClass(JournalAudit.class);
        verify(journalRepository).save(trace.capture());
        assertEquals(ActionAudit.MODIFICATION_IDENTITE, trace.getValue().getAction());
        assertEquals("Téléphone : — → 0341234567", trace.getValue().getDetails());
        assertEquals("0341234567", employe.getTelephone());
    }

    @Test
    void refuseUnEmailDejaUtiliseParUnAutreSalarie() {
        EmployeEmbauche autre = new EmployeEmbauche();
        autre.setId(2L);
        autre.setMatricule("EMP-2026-0002");
        when(employeRepository.findByEmailIgnoreCase("jean.rakoto@mail.com")).thenReturn(Optional.of(autre));

        ApiException erreur = assertThrows(ApiException.class,
                () -> service.modifierIdentite(1L, identite("Rakoto", null), "rh@rh.com"));
        assertEquals("EMAIL_DEJA_UTILISE", erreur.getCode());
        verify(journalRepository, never()).save(any());
    }

    @Test
    void refuseUnFichierDontLeContenuNeCorrespondPasALExtension() {
        MockMultipartFile fauxPdf = new MockMultipartFile("fichier", "cin.pdf", "application/pdf",
                "MZ-executable".getBytes());

        ApiException erreur = assertThrows(ApiException.class, () -> service.ajouterDocument(1L, TypeDocument.CIN,
                null, null, null, null, fauxPdf, "rh@rh.com"));
        assertEquals("FORMAT_INVALIDE", erreur.getCode());
    }

    @Test
    void ajouteUnDocumentPdfEtLeTrace() {
        MockMultipartFile pdf = new MockMultipartFile("fichier", "C:\\scan\\cin.pdf", "application/pdf",
                "%PDF-1.7 contenu".getBytes());

        var document = service.ajouterDocument(1L, TypeDocument.CIN, " ", "101 211 000 111",
                LocalDate.of(2020, 5, 1), AUJOURDHUI.plusDays(10), pdf, "rh@rh.com");

        assertEquals("Carte d'identité nationale", document.libelle());
        assertEquals("cin.pdf", document.nomFichier());
        assertEquals(EtatValidite.EXPIRE_BIENTOT, document.etatValidite());
        assertTrue(document.fichierDisponible());
        verify(documentRepository).save(any(DocumentSalarie.class));
        ArgumentCaptor<JournalAudit> trace = ArgumentCaptor.forClass(JournalAudit.class);
        verify(journalRepository).save(trace.capture());
        assertEquals(ActionAudit.AJOUT_DOCUMENT, trace.getValue().getAction());
        assertNotNull(trace.getValue().getDetails());
    }

    private static Contrat contrat(String reference, TypeContrat type, LocalDate debut, String salaire, StatutContrat statut) {
        Contrat contrat = new Contrat();
        contrat.setReference(reference);
        contrat.setTypeContrat(type);
        contrat.setDateDebut(debut);
        contrat.setSalaire(new BigDecimal(salaire));
        contrat.setStatutContrat(statut);
        return contrat;
    }

    private static IdentiteRequest identite(String nom, String telephone) {
        return new IdentiteRequest(nom, "Jean", "jean.rakoto@mail.com", telephone, null,
                null, null, null, null, null, null, null, null, null, null);
    }
}
