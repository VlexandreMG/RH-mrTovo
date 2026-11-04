package com.mrtovo.backend.service;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculTempsTest {

    private static final CalculTemps.HoraireJour BUREAU =
            new CalculTemps.HoraireJour(LocalTime.of(8, 0), LocalTime.of(17, 0), 60);
    private static final int TOLERANCE = 10;

    private static CalculTemps.ResultatJour jour(String entree, String sortie) {
        return CalculTemps.calculerJour(BUREAU,
                entree != null ? LocalTime.parse(entree) : null,
                sortie != null ? LocalTime.parse(sortie) : null,
                TOLERANCE);
    }

    @Test
    void uneJourneeNormaleDonneHuitHeuresSansRetard() {
        assertEquals(new CalculTemps.ResultatJour(480, 480, 0, 0, 0), jour("08:00", "17:00"));
    }

    @Test
    void unRetardDansLaToleranceEstPardonne() {
        assertEquals(0, jour("08:10", "17:00").retard());
    }

    @Test
    void auDelaDeLaToleranceToutLeRetardCompte() {
        assertEquals(11, jour("08:11", "17:00").retard());
    }

    @Test
    void uneSortieTardiveDonneDesHeuresSup() {
        CalculTemps.ResultatJour resultat = jour("08:00", "19:30");
        assertEquals(150, resultat.heuresSup());
        assertEquals(630, resultat.travaillees());
    }

    @Test
    void uneArriveeEnAvanceNeDonnePasDHeuresSup() {
        assertEquals(0, jour("07:00", "17:00").heuresSup());
    }

    @Test
    void unDepartAnticipeEstCompte() {
        assertEquals(60, jour("08:00", "16:00").departAnticipe());
    }

    @Test
    void laPauseNestPasDeduiteDUneDemiJournee() {
        CalculTemps.ResultatJour resultat = jour("08:00", "12:00");
        assertEquals(240, resultat.travaillees());
        // 8 h prevues - 4 h travaillees : le depart ne doit pas compter aussi l'heure de pause.
        assertEquals(240, resultat.departAnticipe());
    }

    @Test
    void uneEntreeSansSortieGardeLeRetardMaisPasLeTravail() {
        CalculTemps.ResultatJour resultat = jour("08:30", null);
        assertEquals(30, resultat.retard());
        assertEquals(0, resultat.travaillees());
    }

    @Test
    void sansPointageRienNEstDeduit() {
        assertEquals(new CalculTemps.ResultatJour(480, 0, 0, 0, 0), jour(null, null));
    }

    @Test
    void unJourDeReposEstEntierementEnHeuresSup() {
        CalculTemps.ResultatJour resultat =
                CalculTemps.calculerJour(null, LocalTime.of(9, 0), LocalTime.of(12, 0), TOLERANCE);
        assertEquals(new CalculTemps.ResultatJour(0, 180, 0, 0, 180), resultat);
    }

    @Test
    void lesHeuresSupCompensentDAbordLesRetards() {
        CalculTemps.Compensation bilan = CalculTemps.compenser(45, 120, 480);
        assertEquals(45, bilan.compensees());
        assertEquals(75, bilan.hsPayees());
        assertEquals(0, bilan.minutesRetenues());
    }

    @Test
    void leRetardRestantEstRetenu() {
        CalculTemps.Compensation bilan = CalculTemps.compenser(120, 30, 480);
        assertEquals(30, bilan.compensees());
        assertEquals(0, bilan.hsPayees());
        assertEquals(90, bilan.minutesRetenues());
    }

    @Test
    void lesHeuresSupPayeesSontRepartiesEntreLesDeuxPaliers() {
        CalculTemps.Compensation bilan = CalculTemps.compenser(60, 600, 480);
        assertEquals(540, bilan.hsPayees());
        assertEquals(480, bilan.hsPalier1());
        assertEquals(60, bilan.hsPalier2());
    }
}
