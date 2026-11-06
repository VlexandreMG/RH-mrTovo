package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.AnomaliePointageDto;
import com.mrtovo.backend.dto.CalendrierCongeDto;
import com.mrtovo.backend.dto.JourTempsDto;
import com.mrtovo.backend.entity.DemandeConge;
import com.mrtovo.backend.entity.Departement;
import com.mrtovo.backend.entity.DepartementRecrutement;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.NatureCaseCalendrier;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.StatutAbsence;
import com.mrtovo.backend.entity.StatutDemandeConge;
import com.mrtovo.backend.entity.TypeConge;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.repository.DemandeCongeRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendrierCongeServiceTest {

    private static final YearMonth MOIS = YearMonth.of(2026, 11);
    private static final LocalDate LUNDI = LocalDate.of(2026, 11, 9);
    private static final String RH = "rh@rh.com";
    private static final String MANAGER = "manager@rh.com";

    private UtilisateurRepository utilisateurRepository;
    private DemandeCongeRepository demandeRepository;
    private TempsTravailService tempsService;
    private CalendrierCongeService service;
    private EmployeEmbauche aina;
    private EmployeEmbauche luc;
    private final List<DemandeConge> demandes = new ArrayList<>();
    private final Set<LocalDate> absencesDeLuc = new HashSet<>();
    private StatutAbsence statutAbsence;

    @BeforeEach
    void preparer() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        demandeRepository = mock(DemandeCongeRepository.class);
        tempsService = mock(TempsTravailService.class);
        service = new CalendrierCongeService(utilisateurRepository, demandeRepository, tempsService);

        aina = employe(1L, "Aina", "Informatique");
        luc = employe(2L, "Luc", "Finance");
        utilisateur(RH, RoleUtilisateur.RH, null);
        utilisateur(MANAGER, RoleUtilisateur.MANAGER, "informatique");

        when(demandeRepository.surPeriode(any(), any(), any())).thenReturn(demandes);
        when(tempsService.joursDeLEffectif(MOIS.atDay(1), MOIS.atEndOfMonth())).thenAnswer(appel -> List.of(
                new TempsTravailService.JoursSalarie(aina, jours(Set.of())),
                new TempsTravailService.JoursSalarie(luc, jours(absencesDeLuc))));
    }

    @Test
    void leRhVoitTousLesSalariesEtLeTypeDeConge() {
        demande(aina, "Congé maladie", StatutDemandeConge.VALIDEE, LUNDI, LUNDI.plusDays(1));

        CalendrierCongeDto calendrier = service.calendrier(MOIS, null, RH);

        assertTrue(calendrier.detailVisible());
        assertEquals(List.of("Finance", "Informatique"), calendrier.departements());
        assertEquals(2, calendrier.salaries().size());
        CalendrierCongeDto.Case premiere = casesDe(calendrier, "Aina Rabe").get(0);
        assertEquals(NatureCaseCalendrier.CONGE, premiere.nature());
        assertEquals("Congé maladie", premiere.libelle());
        assertEquals(10L, premiere.demandeId());
        assertEquals(LUNDI.plusDays(1), premiere.dateFin());
        assertEquals(2, casesDe(calendrier, "Aina Rabe").size());
    }

    @Test
    void leRhPeutFiltrerParDepartementSansPerdreLaListeDesDepartements() {
        CalendrierCongeDto calendrier = service.calendrier(MOIS, "finance", RH);

        assertEquals(1, calendrier.salaries().size());
        assertEquals("Luc Rabe", calendrier.salaries().get(0).nomComplet());
        assertEquals(List.of("Finance", "Informatique"), calendrier.departements());
    }

    @Test
    void unManagerResteSurSonDepartementMemeSIlDemandeUnAutre() {
        CalendrierCongeDto calendrier = service.calendrier(MOIS, "Finance", MANAGER);

        assertFalse(calendrier.detailVisible());
        assertEquals(List.of("Informatique"), calendrier.departements());
        assertEquals(1, calendrier.salaries().size());
        assertEquals("Aina Rabe", calendrier.salaries().get(0).nomComplet());
    }

    @Test
    void unManagerSansDepartementNeVoitPersonne() {
        utilisateur("seul@rh.com", RoleUtilisateur.MANAGER, null);

        assertTrue(service.calendrier(MOIS, null, "seul@rh.com").salaries().isEmpty());
    }

    @Test
    void unManagerNeRecoitNiLeTypeNiLaDemande() {
        demande(aina, "Congé maladie", StatutDemandeConge.VALIDEE, LUNDI, LUNDI);

        CalendrierCongeDto.Case absent = casesDe(service.calendrier(MOIS, null, MANAGER), "Aina Rabe").get(0);

        assertEquals(NatureCaseCalendrier.ABSENT, absent.nature());
        assertEquals("Absent", absent.libelle());
        assertNull(absent.demandeId());
        assertNull(absent.typeCongeId());
        assertNull(absent.statut());
        assertNull(absent.dateDebut());
        assertNull(absent.dateFin());
    }

    @Test
    void unManagerVoitLesDemandesEnAttenteSansLeurType() {
        demande(aina, "Congé maladie", StatutDemandeConge.EN_ATTENTE_MANAGER, LUNDI, LUNDI);

        CalendrierCongeDto.Case attente = casesDe(service.calendrier(MOIS, null, MANAGER), "Aina Rabe").get(0);

        assertEquals(NatureCaseCalendrier.EN_ATTENTE, attente.nature());
        assertEquals("Absence en attente", attente.libelle());
        assertNull(attente.demandeId());
    }

    @Test
    void uneAbsenceQualifieeEstMasqueeAuManager() {
        utilisateur("finance@rh.com", RoleUtilisateur.MANAGER, "Finance");
        absencesDeLuc.add(LUNDI);
        statutAbsence = StatutAbsence.NON_JUSTIFIEE;

        assertEquals(NatureCaseCalendrier.ABSENCE_NON_JUSTIFIEE,
                casesDe(service.calendrier(MOIS, null, RH), "Luc Rabe").get(0).nature());
        assertEquals(NatureCaseCalendrier.ABSENT,
                casesDe(service.calendrier(MOIS, null, "finance@rh.com"), "Luc Rabe").get(0).nature());
    }

    @Test
    void unCongeValideRetireLeSalarieDesPresents() {
        demande(aina, "Congé payé", StatutDemandeConge.VALIDEE, LUNDI, LUNDI);

        CalendrierCongeDto.Synthese jour = synthese(service.calendrier(MOIS, null, RH), LUNDI);

        assertEquals(2, jour.attendus());
        assertEquals(1, jour.presents());
        assertEquals(0, jour.enAttente());
    }

    @Test
    void uneDemandeEnAttenteLaisseLeSalarieParmiLesPresentsEtSeCompteAPart() {
        demande(aina, "Congé payé", StatutDemandeConge.EN_ATTENTE_RH, LUNDI, LUNDI);

        CalendrierCongeDto.Synthese jour = synthese(service.calendrier(MOIS, null, RH), LUNDI);

        assertEquals(2, jour.presents());
        assertEquals(1, jour.enAttente());
    }

    @Test
    void lesTotauxSuiventLeFiltreDepartement() {
        demande(aina, "Congé payé", StatutDemandeConge.VALIDEE, LUNDI, LUNDI);

        CalendrierCongeDto.Synthese jour = synthese(service.calendrier(MOIS, "Informatique", RH), LUNDI);

        assertEquals(1, jour.attendus());
        assertEquals(0, jour.presents());
    }

    @Test
    void uneAbsenceSansQualificationResteAQualifierEtNeCompteQuUneFois() {
        absencesDeLuc.add(LUNDI);

        CalendrierCongeDto calendrier = service.calendrier(MOIS, null, RH);

        assertEquals(NatureCaseCalendrier.ABSENCE_A_QUALIFIER, casesDe(calendrier, "Luc Rabe").get(0).nature());
        assertEquals(1, synthese(calendrier, LUNDI).presents());
    }

    @Test
    void uneDemandeEnAttentePasseAvantLAbsenceDuMemeJour() {
        absencesDeLuc.add(LUNDI);
        demande(luc, "Congé maladie", StatutDemandeConge.EN_ATTENTE_RH, LUNDI, LUNDI);

        CalendrierCongeDto calendrier = service.calendrier(MOIS, null, RH);

        assertEquals(NatureCaseCalendrier.EN_ATTENTE, casesDe(calendrier, "Luc Rabe").get(0).nature());
        assertEquals(1, synthese(calendrier, LUNDI).presents());
        assertEquals(0, synthese(calendrier, LUNDI).enAttente());
    }

    @Test
    void personneNEstAttenduUnJourDeRepos() {
        LocalDate samedi = LUNDI.plusDays(5);
        demande(aina, "Congé payé", StatutDemandeConge.VALIDEE, LUNDI, LUNDI.plusDays(6));

        CalendrierCongeDto calendrier = service.calendrier(MOIS, null, RH);

        assertEquals(0, synthese(calendrier, samedi).attendus());
        assertEquals(7, casesDe(calendrier, "Aina Rabe").size());
    }

    // =====================================================================
    // Outils
    // =====================================================================

    private List<CalendrierCongeDto.Case> casesDe(CalendrierCongeDto calendrier, String nom) {
        return calendrier.salaries().stream()
                .filter(ligne -> ligne.nomComplet().equals(nom))
                .findFirst()
                .orElseThrow()
                .cases();
    }

    private CalendrierCongeDto.Synthese synthese(CalendrierCongeDto calendrier, LocalDate date) {
        return calendrier.jours().stream().filter(jour -> jour.date().equals(date)).findFirst().orElseThrow();
    }

    private void demande(EmployeEmbauche employe, String libelle, StatutDemandeConge statut, LocalDate debut, LocalDate fin) {
        TypeConge type = new TypeConge();
        type.setId(3L);
        type.setLibelle(libelle);
        DemandeConge demande = new DemandeConge();
        demande.setId(10L + demandes.size());
        demande.setEmploye(employe);
        demande.setTypeConge(type);
        demande.setStatut(statut);
        demande.setDateDebut(debut);
        demande.setDateFin(fin);
        demandes.add(demande);
    }

    private void utilisateur(String email, RoleUtilisateur role, String nomDepartement) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setRole(role);
        if (nomDepartement != null) {
            Departement departement = new Departement();
            departement.setNom(nomDepartement);
            utilisateur.setDepartement(departement);
        }
        when(utilisateurRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(utilisateur));
    }

    private static EmployeEmbauche employe(Long id, String prenom, String nomDepartement) {
        DepartementRecrutement departement = new DepartementRecrutement();
        departement.setNom(nomDepartement);
        EmployeEmbauche employe = new EmployeEmbauche();
        employe.setId(id);
        employe.setPrenom(prenom);
        employe.setNom("Rabe");
        employe.setMatricule("EMP-" + id);
        employe.setDepartement(departement);
        return employe;
    }

    /** Le mois au regime de bureau : prevu du lundi au vendredi. */
    private List<JourTempsDto> jours(Set<LocalDate> absences) {
        List<JourTempsDto> jours = new ArrayList<>();
        for (LocalDate date = MOIS.atDay(1); !date.isAfter(MOIS.atEndOfMonth()); date = date.plusDays(1)) {
            boolean ouvre = date.getDayOfWeek().getValue() < DayOfWeek.SATURDAY.getValue();
            boolean absent = absences.contains(date);
            jours.add(new JourTempsDto(date, ouvre ? LocalTime.of(8, 0) : null, ouvre ? LocalTime.of(17, 0) : null,
                    null, null, null, null, absent, null, null, null, absent ? statutAbsence : null,
                    0, 0, 0, 0, 0, List.<AnomaliePointageDto>of()));
        }
        return jours;
    }
}
