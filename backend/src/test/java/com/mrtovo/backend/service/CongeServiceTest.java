package com.mrtovo.backend.service;

import com.mrtovo.backend.config.CongeProperties;
import com.mrtovo.backend.dto.AjustementDroitRequest;
import com.mrtovo.backend.dto.DecisionCongeRequest;
import com.mrtovo.backend.dto.DemandeCongeDto;
import com.mrtovo.backend.dto.IndemnisationRequest;
import com.mrtovo.backend.dto.SoldeCongeDto;
import com.mrtovo.backend.entity.Contrat;
import com.mrtovo.backend.entity.DemandeConge;
import com.mrtovo.backend.entity.Departement;
import com.mrtovo.backend.entity.DepartementRecrutement;
import com.mrtovo.backend.entity.DroitConge;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.StatutContrat;
import com.mrtovo.backend.entity.StatutDemandeConge;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.TypeConge;
import com.mrtovo.backend.entity.TypeContrat;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.ContratRepository;
import com.mrtovo.backend.repository.DemandeCongeRepository;
import com.mrtovo.backend.repository.DroitCongeRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.TypeCongeRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CongeServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 5);
    private static final LocalDate LUNDI_PROCHAIN = LocalDate.of(2026, 11, 9);

    private DemandeCongeRepository demandeRepository;
    private TypeCongeRepository typeRepository;
    private EmployeEmbaucheRepository employeRepository;
    private UtilisateurRepository utilisateurRepository;
    private DroitCongeRepository droitRepository;
    private ContratRepository contratRepository;
    private SoldeCongeService soldeService;
    private CongeService service;
    private EmployeEmbauche employe;
    private TypeConge congePaye;
    private TypeConge maladie;
    private final List<DemandeConge> demandes = new ArrayList<>();
    private long prochainId = 100;

    @TempDir
    Path dossierTemporaire;

    @BeforeEach
    void preparer() {
        demandeRepository = mock(DemandeCongeRepository.class);
        typeRepository = mock(TypeCongeRepository.class);
        employeRepository = mock(EmployeEmbaucheRepository.class);
        utilisateurRepository = mock(UtilisateurRepository.class);
        droitRepository = mock(DroitCongeRepository.class);
        contratRepository = mock(ContratRepository.class);
        TraceTempsService trace = mock(TraceTempsService.class);
        Clock horloge = Clock.fixed(AUJOURDHUI.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        soldeService = new SoldeCongeService(employeRepository, demandeRepository, droitRepository, contratRepository,
                utilisateurRepository, trace, new CongeProperties(), horloge);
        service = new CongeService(demandeRepository, typeRepository, employeRepository, utilisateurRepository,
                soldeService, new DocumentStorageService(dossierTemporaire.toString()), trace, horloge);

        DepartementRecrutement informatique = new DepartementRecrutement();
        informatique.setNom("Informatique");

        // Entre le 05/07/2026 : 4 mois complets, donc 10 jours acquis.
        employe = new EmployeEmbauche();
        employe.setId(1L);
        employe.setMatricule("EMP-1");
        employe.setNom("Rabe");
        employe.setPrenom("Aina");
        employe.setDepartement(informatique);
        employe.setDateEntree(LocalDate.of(2026, 7, 5));
        employe.setStatut(StatutEmploye.ACTIF);

        congePaye = type(1L, true, true, false, null);
        maladie = type(2L, false, true, true, null);

        when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(employeRepository.verrouiller(1L)).thenReturn(Optional.of(employe));
        when(typeRepository.findById(1L)).thenReturn(Optional.of(congePaye));
        when(typeRepository.findById(2L)).thenReturn(Optional.of(maladie));
        when(utilisateurRepository.existsByRoleAndDepartementNomIgnoreCase(RoleUtilisateur.MANAGER, "Informatique"))
                .thenReturn(true);
        when(utilisateurRepository.findByEmployeId(1L)).thenReturn(Optional.empty());
        when(demandeRepository.findByEmployeIdOrderByDateDebutDesc(1L)).thenReturn(demandes);
        when(demandeRepository.chevauchantes(anyLong(), any(), any(), any())).thenAnswer(appel -> {
            LocalDate debut = appel.getArgument(1);
            LocalDate fin = appel.getArgument(2);
            return demandes.stream()
                    .filter(d -> StatutDemandeConge.ACTIFS.contains(d.getStatut()))
                    .filter(d -> !d.getDateDebut().isAfter(fin) && !d.getDateFin().isBefore(debut))
                    .toList();
        });
        when(demandeRepository.save(any())).thenAnswer(appel -> {
            DemandeConge demande = appel.getArgument(0);
            demande.setId(prochainId++);
            demandes.add(demande);
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            return demande;
        });
        when(droitRepository.findByEmployeIdOrderByDateCreationDesc(1L)).thenReturn(List.of());
        when(contratRepository.findByEmployeIdOrderByDateDebutAsc(1L)).thenReturn(List.of(contrat("1200000")));
    }

    @Test
    void leSoldeDeduitLesCongesValidesEtLesDemandesEnAttente() {
        demandeExistante(StatutDemandeConge.VALIDEE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
        demandeExistante(StatutDemandeConge.EN_ATTENTE_RH, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2));
        demandeExistante(StatutDemandeConge.REFUSEE, LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 11));

        SoldeCongeDto solde = soldeService.solde(1L);

        assertEquals(0, new BigDecimal("10").compareTo(solde.joursAcquis()));
        assertEquals(0, new BigDecimal("7").compareTo(solde.solde()));
        assertEquals(0, new BigDecimal("5").compareTo(solde.disponible()));
        // Formule du cours : 1 200 000 x 7 / 30.
        assertEquals(new BigDecimal("280000.00"), solde.indemniteCompensatrice());
    }

    @Test
    void uneDemandeValablePartChezLeManager() {
        DemandeCongeDto demande = demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN.plusDays(4));

        assertEquals(StatutDemandeConge.EN_ATTENTE_MANAGER, demande.statut());
        assertEquals(5, demande.nbJours());
    }

    @Test
    void sansManagerDansLeDepartementLaDemandeVaDirectementAuRh() {
        when(utilisateurRepository.existsByRoleAndDepartementNomIgnoreCase(any(), any())).thenReturn(false);

        assertEquals(StatutDemandeConge.EN_ATTENTE_RH, demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN).statut());
    }

    @Test
    void refuseUneDemandeSuperieureAuSoldeDisponible() {
        ApiException erreur = assertThrows(ApiException.class,
                () -> demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN.plusDays(10)));

        assertEquals("SOLDE_INSUFFISANT", erreur.getCode());
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void refuseUneDemandeQuiChevaucheUneAutre() {
        demandeExistante(StatutDemandeConge.EN_ATTENTE_RH, LUNDI_PROCHAIN.plusDays(2), LUNDI_PROCHAIN.plusDays(3));

        ApiException erreur = assertThrows(ApiException.class,
                () -> demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN.plusDays(2)));

        assertEquals("CHEVAUCHEMENT", erreur.getCode());
    }

    @Test
    void uneDemandeRefuseeNeBloquePasSesDates() {
        demandeExistante(StatutDemandeConge.REFUSEE, LUNDI_PROCHAIN, LUNDI_PROCHAIN.plusDays(1));

        assertEquals(StatutDemandeConge.EN_ATTENTE_MANAGER, demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN).statut());
    }

    @Test
    void unCongePayeNeSeDemandePasApresCoupMaisUneMaladieSi() {
        LocalDate hier = AUJOURDHUI.minusDays(1);

        ApiException erreur = assertThrows(ApiException.class, () -> demander(hier, hier));
        assertEquals("DATE_PASSEE", erreur.getCode());

        DemandeCongeDto declaration = service.demander(1L, 2L, hier, hier, "Grippe", null, "aina@rh.com");
        assertEquals(StatutDemandeConge.EN_ATTENTE_MANAGER, declaration.statut());
    }

    @Test
    void unManagerNePeutPasDonnerSonAvisPourUnAutreDepartement() {
        DemandeCongeDto demande = demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN);
        utilisateur("manager.finance@rh.com", RoleUtilisateur.MANAGER, "Finance");

        ApiException erreur = assertThrows(ApiException.class, () -> service.avisManager(demande.id(),
                new DecisionCongeRequest(true, null), "manager.finance@rh.com"));

        assertEquals("AUTRE_DEPARTEMENT", erreur.getCode());
    }

    @Test
    void leParcoursCompletMeneALaValidation() {
        DemandeCongeDto demande = demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN.plusDays(4));
        utilisateur("manager.info@rh.com", RoleUtilisateur.MANAGER, "Informatique");

        DemandeCongeDto apresManager = service.avisManager(demande.id(),
                new DecisionCongeRequest(true, null), "manager.info@rh.com");
        assertEquals(StatutDemandeConge.EN_ATTENTE_RH, apresManager.statut());

        DemandeCongeDto validee = service.decisionRh(demande.id(), new DecisionCongeRequest(true, null), "rh@rh.com");
        assertEquals(StatutDemandeConge.VALIDEE, validee.statut());
        assertEquals(0, new BigDecimal("5").compareTo(soldeService.solde(1L).solde()));
    }

    @Test
    void leRhNePeutPasPasserAvantLeManager() {
        DemandeCongeDto demande = demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN);

        ApiException erreur = assertThrows(ApiException.class,
                () -> service.decisionRh(demande.id(), new DecisionCongeRequest(true, null), "rh@rh.com"));

        assertEquals("ETAPE_INVALIDE", erreur.getCode());
    }

    @Test
    void unRefusDoitEtreJustifie() {
        DemandeCongeDto demande = demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN);
        utilisateur("manager.info@rh.com", RoleUtilisateur.MANAGER, "Informatique");

        ApiException erreur = assertThrows(ApiException.class, () -> service.avisManager(demande.id(),
                new DecisionCongeRequest(false, " "), "manager.info@rh.com"));

        assertEquals("COMMENTAIRE_OBLIGATOIRE", erreur.getCode());
    }

    @Test
    void leRhNeValidePasUneMaladieSansJustificatif() {
        DemandeCongeDto demande = service.demander(1L, 2L, AUJOURDHUI, AUJOURDHUI, null, null, "aina@rh.com");
        demandes.get(0).setStatut(StatutDemandeConge.EN_ATTENTE_RH);

        ApiException erreur = assertThrows(ApiException.class,
                () -> service.decisionRh(demande.id(), new DecisionCongeRequest(true, null), "rh@rh.com"));

        assertEquals("JUSTIFICATIF_MANQUANT", erreur.getCode());
    }

    @Test
    void annulerRendLesJoursAuSolde() {
        DemandeCongeDto demande = demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN.plusDays(4));
        assertEquals(0, new BigDecimal("5").compareTo(soldeService.solde(1L).disponible()));

        service.annulerParSalarie(1L, demande.id(), "aina@rh.com");

        assertEquals(0, new BigDecimal("10").compareTo(soldeService.solde(1L).disponible()));
    }

    @Test
    void unCongeCommenceNeSAnnulePlus() {
        DemandeConge demande = demandeExistante(StatutDemandeConge.VALIDEE, AUJOURDHUI, AUJOURDHUI.plusDays(1));
        when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));

        ApiException erreur = assertThrows(ApiException.class,
                () -> service.annulerParSalarie(1L, demande.getId(), "aina@rh.com"));

        assertEquals("CONGE_COMMENCE", erreur.getCode());
    }

    @Test
    void unSalarieNeVoitPasLaDemandeDUnAutre() {
        DemandeCongeDto demande = demander(LUNDI_PROCHAIN, LUNDI_PROCHAIN);

        ApiException erreur = assertThrows(ApiException.class,
                () -> service.annulerParSalarie(2L, demande.id(), "autre@rh.com"));

        assertEquals("INTROUVABLE", erreur.getCode());
    }

    @Test
    void indemniserPaieLesJoursNonPrisEtFigeLeMontant() {
        ArgumentCaptor<DroitConge> droit = ArgumentCaptor.forClass(DroitConge.class);

        soldeService.indemniser(1L, new IndemnisationRequest(new BigDecimal("4"), "2026-11", "Fin de contrat"), "rh@rh.com");

        verify(droitRepository).save(droit.capture());
        assertEquals(new BigDecimal("160000.00"), droit.getValue().getMontant());
        assertEquals(0, new BigDecimal("-4").compareTo(droit.getValue().getJours()));
    }

    @Test
    void onNePeutPasRetirerPlusQueLeDisponible() {
        ApiException erreur = assertThrows(ApiException.class, () -> soldeService.ajuster(1L,
                new AjustementDroitRequest(new BigDecimal("-11"), "Erreur"), "rh@rh.com"));

        assertEquals("SOLDE_INSUFFISANT", erreur.getCode());
    }

    private DemandeCongeDto demander(LocalDate debut, LocalDate fin) {
        return service.demander(1L, 1L, debut, fin, null, null, "aina@rh.com");
    }

    private DemandeConge demandeExistante(StatutDemandeConge statut, LocalDate debut, LocalDate fin) {
        DemandeConge demande = new DemandeConge();
        demande.setId(prochainId++);
        demande.setEmploye(employe);
        demande.setTypeConge(congePaye);
        demande.setDateDebut(debut);
        demande.setDateFin(fin);
        demande.setNbJours(CalculConge.joursCalendaires(debut, fin));
        demande.setStatut(statut);
        demandes.add(demande);
        return demande;
    }

    private void utilisateur(String email, RoleUtilisateur role, String departement) {
        Departement dep = new Departement();
        dep.setNom(departement);
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setRole(role);
        utilisateur.setDepartement(dep);
        when(utilisateurRepository.findByEmailIgnoreCase(eq(email))).thenReturn(Optional.of(utilisateur));
    }

    private static TypeConge type(Long id, boolean decompte, boolean remunere, boolean justificatif, Integer plafond) {
        TypeConge type = new TypeConge();
        type.setId(id);
        type.setLibelle("Type " + id);
        type.setDecompteSolde(decompte);
        type.setRemunere(remunere);
        type.setJustificatifRequis(justificatif);
        type.setPlafondAnnuelJours(plafond);
        return type;
    }

    private static Contrat contrat(String salaire) {
        Contrat contrat = new Contrat();
        contrat.setReference("CTR");
        contrat.setTypeContrat(TypeContrat.CDI);
        contrat.setDateDebut(LocalDate.of(2026, 7, 5));
        contrat.setStatutContrat(StatutContrat.ACTIF);
        contrat.setSalaire(new BigDecimal(salaire));
        return contrat;
    }
}
