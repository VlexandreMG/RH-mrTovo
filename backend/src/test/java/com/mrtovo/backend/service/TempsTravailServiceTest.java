package com.mrtovo.backend.service;

import com.mrtovo.backend.config.TempsTravailProperties;
import com.mrtovo.backend.dto.AnomaliePointageDto;
import com.mrtovo.backend.dto.CorrectionPointageRequest;
import com.mrtovo.backend.dto.DecisionHeuresSupRequest;
import com.mrtovo.backend.dto.SemaineTempsDto;
import com.mrtovo.backend.dto.VariablesPaieDto;
import com.mrtovo.backend.entity.Contrat;
import com.mrtovo.backend.entity.DecisionHeuresSup;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.HoraireRegime;
import com.mrtovo.backend.entity.PlanningSalarie;
import com.mrtovo.backend.entity.Pointage;
import com.mrtovo.backend.entity.RegimeTravail;
import com.mrtovo.backend.entity.StatutContrat;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.StatutHeuresSup;
import com.mrtovo.backend.entity.TypeAnomaliePointage;
import com.mrtovo.backend.entity.TypeContrat;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.ContratRepository;
import com.mrtovo.backend.repository.DecisionHeuresSupRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.PlanningSalarieRepository;
import com.mrtovo.backend.repository.PointageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TempsTravailServiceTest {

    /** Mercredi : la semaine du lundi 26/10/2026 est terminee, celle du 02/11 ne l'est pas. */
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 4);
    private static final LocalDate LUNDI = LocalDate.of(2026, 10, 26);

    private EmployeEmbaucheRepository employeRepository;
    private PointageRepository pointageRepository;
    private PlanningSalarieRepository planningRepository;
    private DecisionHeuresSupRepository decisionRepository;
    private ContratRepository contratRepository;
    private TraceTempsService trace;
    private TempsTravailService temps;
    private PointageService pointages;
    private EmployeEmbauche employe;
    private final List<Pointage> enBase = new ArrayList<>();

    @BeforeEach
    void preparer() {
        employeRepository = mock(EmployeEmbaucheRepository.class);
        pointageRepository = mock(PointageRepository.class);
        planningRepository = mock(PlanningSalarieRepository.class);
        decisionRepository = mock(DecisionHeuresSupRepository.class);
        contratRepository = mock(ContratRepository.class);
        trace = mock(TraceTempsService.class);
        Clock horloge = Clock.fixed(AUJOURDHUI.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        temps = new TempsTravailService(employeRepository, pointageRepository, planningRepository,
                decisionRepository, contratRepository, new TempsTravailProperties(), horloge);
        pointages = new PointageService(employeRepository, pointageRepository, decisionRepository, temps, trace, horloge);

        employe = new EmployeEmbauche();
        employe.setId(1L);
        employe.setMatricule("EMP-1");
        employe.setNom("Rabe");
        employe.setPrenom("Aina");
        employe.setDateEntree(LocalDate.of(2025, 1, 6));
        employe.setStatut(StatutEmploye.ACTIF);

        when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(employeRepository.getReferenceById(1L)).thenReturn(employe);
        when(employeRepository.findAll()).thenReturn(List.of(employe));
        when(pointageRepository.findByDatePointageBetween(any(), any())).thenReturn(enBase);
        when(decisionRepository.findBySemaineDebutBetween(any(), any())).thenReturn(List.of());
        when(decisionRepository.findByEmployeIdAndSemaineDebut(any(), any())).thenReturn(Optional.empty());
        when(planningRepository.chevauchant(any(), any())).thenReturn(List.of(planningBureau()));
    }

    @Test
    void laSemaineCompenseLeRetardDuLundiParLesHeuresSupValideesDuJeudi() {
        semaineTravaillee();
        pointer(LUNDI, "08:45", "17:00");            // 45 min de retard
        pointer(LUNDI.plusDays(3), "08:00", "19:00"); // 2 h d'heures sup
        decisionValidee(120);

        SemaineTempsDto semaine = temps.semaine(1L, LUNDI.plusDays(2));

        assertEquals(LUNDI, semaine.semaineDebut());
        assertEquals(45, semaine.minutesRetard());
        assertEquals(120, semaine.decision().minutesBrutes());
        assertEquals(StatutHeuresSup.VALIDEE, semaine.decision().statut());
        assertEquals(45, semaine.bilan().minutesCompensees());
        assertEquals(75, semaine.bilan().hsPayees());
        assertEquals(0, semaine.bilan().minutesRetenues());
    }

    @Test
    void desHeuresSupNonValideesNeCompensentRien() {
        semaineTravaillee();
        pointer(LUNDI, "08:45", "17:00");
        pointer(LUNDI.plusDays(3), "08:00", "19:00");

        SemaineTempsDto semaine = temps.semaine(1L, LUNDI);

        assertEquals(StatutHeuresSup.A_VALIDER, semaine.decision().statut());
        assertEquals(0, semaine.bilan().minutesCompensees());
        assertEquals(45, semaine.bilan().minutesRetenues());
        assertEquals(0, semaine.bilan().hsPayees());
    }

    @Test
    void detecteLesAnomaliesDePointage() {
        pointer(LUNDI, "09:30", "17:00");             // retard de 90 min
        pointer(LUNDI.plusDays(1), "08:00", null);    // sortie oubliee
        pointer(LUNDI.plusDays(2), "08:00", "15:00"); // depart anticipe
        pointer(LUNDI.plusDays(5), "09:00", "12:00"); // samedi : repos
        // Jeudi et vendredi : aucun pointage.

        List<TypeAnomaliePointage> types = temps.anomalies(LUNDI, LUNDI.plusDays(6)).stream()
                .map(AnomaliePointageDto::type)
                .toList();

        assertTrue(types.contains(TypeAnomaliePointage.RETARD_EXCESSIF));
        assertTrue(types.contains(TypeAnomaliePointage.SORTIE_MANQUANTE));
        assertTrue(types.contains(TypeAnomaliePointage.DEPART_ANTICIPE));
        assertTrue(types.contains(TypeAnomaliePointage.TRAVAIL_JOUR_REPOS));
        assertEquals(2, types.stream().filter(TypeAnomaliePointage.POINTAGE_MANQUANT::equals).count());
    }

    @Test
    void aujourdhuiEtLesJoursAvenirNeSontPasDesAbsences() {
        SemaineTempsDto semaine = temps.semaine(1L, AUJOURDHUI);
        // Lundi et mardi sont passes ; mercredi (aujourd'hui), jeudi et vendredi ne comptent pas.
        assertEquals(2, semaine.joursAbsents());
    }

    @Test
    void unSalarieSansRegimeEstSignaleSansInventerDeRetard() {
        when(planningRepository.chevauchant(any(), any())).thenReturn(List.of());
        pointer(LUNDI, "09:30", "17:00");

        SemaineTempsDto semaine = temps.semaine(1L, LUNDI);

        assertEquals(0, semaine.minutesRetard());
        assertEquals(TypeAnomaliePointage.SANS_REGIME, semaine.jours().get(0).anomalies().get(0).type());
    }

    @Test
    void onNePeutPasDeciderSurUneSemaineEnCours() {
        pointer(AUJOURDHUI.minusDays(1), "08:00", "19:00");
        LocalDate lundiCourant = LocalDate.of(2026, 11, 2);

        ApiException erreur = assertThrows(ApiException.class, () -> pointages.deciderHeuresSup(
                new DecisionHeuresSupRequest(1L, lundiCourant, StatutHeuresSup.VALIDEE, 120, null), "rh@rh.com"));

        assertEquals("HEURES_SUP_NON_DECIDABLES", erreur.getCode());
        verify(decisionRepository, never()).save(any());
    }

    @Test
    void uneValidationPartielleDoitEtreJustifiee() {
        semaineTravaillee();
        pointer(LUNDI, "08:00", "19:00");

        ApiException erreur = assertThrows(ApiException.class, () -> pointages.deciderHeuresSup(
                new DecisionHeuresSupRequest(1L, LUNDI, StatutHeuresSup.VALIDEE, 60, null), "rh@rh.com"));

        assertEquals("COMMENTAIRE_OBLIGATOIRE", erreur.getCode());
    }

    @Test
    void corrigerUnPointageRouvreLaDecisionDeLaSemaine() {
        Pointage pointage = pointer(LUNDI, "08:00", "19:00");
        when(pointageRepository.findById(10L)).thenReturn(Optional.of(pointage));

        pointages.corriger(10L, new CorrectionPointageRequest(
                LocalTime.of(8, 0), LocalTime.of(18, 0), null, "Erreur de saisie"), "rh@rh.com");

        verify(trace).rouvrirDecision(any(), org.mockito.ArgumentMatchers.eq(LUNDI), any(), any());
        assertEquals(LocalTime.of(18, 0), pointage.getHeureSortie());
        assertEquals("rh@rh.com", pointage.getModifiePar());
    }

    @Test
    void uneCorrectionSansChangementNeRouvreRien() {
        Pointage pointage = pointer(LUNDI, "08:00", "19:00");
        when(pointageRepository.findById(10L)).thenReturn(Optional.of(pointage));

        pointages.corriger(10L, new CorrectionPointageRequest(
                LocalTime.of(8, 0), LocalTime.of(19, 0), null, "Rien"), "rh@rh.com");

        verify(trace, never()).rouvrirDecision(any(), any(), any(), any());
    }

    @Test
    void lesVariablesDePaieCalculentLesMontantsAvecLaMajoration() {
        semaineTravaillee();
        pointer(LUNDI, "08:00", "20:00"); // 3 h d'heures sup
        decisionValidee(180);
        Contrat contrat = new Contrat();
        contrat.setReference("CTR");
        contrat.setTypeContrat(TypeContrat.CDI);
        contrat.setDateDebut(LocalDate.of(2025, 1, 6));
        contrat.setStatutContrat(StatutContrat.ACTIF);
        contrat.setSalaire(new BigDecimal("2080000"));
        when(contratRepository.findByEmployeIdOrderByDateDebutAsc(1L)).thenReturn(List.of(contrat));

        VariablesPaieDto variables = temps.variablesPaie(YearMonth.of(2026, 10)).get(0);

        assertEquals(180, variables.hsPalier1());
        assertEquals(0, variables.hsPalier2());
        // 2 080 000 x 12 / (52 x 40 h) = 12 000 Ar/h ; 3 h a 130 % = 46 800 Ar.
        assertEquals(new BigDecimal("12000.00"), variables.tauxHoraire());
        assertEquals(new BigDecimal("46800.00"), variables.montantHeuresSup());
        assertFalse(variables.provisoire());
    }

    private void semaineTravaillee() {
        for (int jour = 0; jour < 5; jour++) {
            pointer(LUNDI.plusDays(jour), "08:00", "17:00");
        }
    }

    private Pointage pointer(LocalDate date, String entree, String sortie) {
        enBase.removeIf(pointage -> pointage.getDatePointage().equals(date));
        Pointage pointage = new Pointage();
        pointage.setId(10L);
        pointage.setEmploye(employe);
        pointage.setDatePointage(date);
        pointage.setHeureEntree(LocalTime.parse(entree));
        pointage.setHeureSortie(sortie != null ? LocalTime.parse(sortie) : null);
        enBase.add(pointage);
        return pointage;
    }

    private void decisionValidee(int minutes) {
        DecisionHeuresSup decision = new DecisionHeuresSup();
        decision.setEmploye(employe);
        decision.setSemaineDebut(LUNDI);
        decision.setStatut(StatutHeuresSup.VALIDEE);
        decision.setMinutesValidees(minutes);
        when(decisionRepository.findBySemaineDebutBetween(any(), any())).thenReturn(List.of(decision));
    }

    private PlanningSalarie planningBureau() {
        RegimeTravail regime = new RegimeTravail();
        regime.setLibelle("Bureau 40 h");
        regime.setToleranceRetardMinutes(10);
        for (int jour = 1; jour <= 5; jour++) {
            HoraireRegime horaire = new HoraireRegime();
            horaire.setJourSemaine(jour);
            horaire.setHeureDebut(LocalTime.of(8, 0));
            horaire.setHeureFin(LocalTime.of(17, 0));
            horaire.setPauseMinutes(60);
            regime.ajouterHoraire(horaire);
        }
        PlanningSalarie planning = new PlanningSalarie();
        planning.setEmploye(employe);
        planning.setRegime(regime);
        planning.setDateDebut(LocalDate.of(2025, 1, 6));
        return planning;
    }
}
