package com.mrtovo.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculCongeTest {

    @Test
    void lesJoursCalendairesIncluentLesDeuxBornesEtLeWeekEnd() {
        assertEquals(1, CalculConge.joursCalendaires(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9)));
        assertEquals(5, CalculConge.joursCalendaires(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13)));
        assertEquals(8, CalculConge.joursCalendaires(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16)));
    }

    @Test
    void seulsLesMoisCompletsOuvrentUnDroit() {
        LocalDate entree = LocalDate.of(2026, 1, 15);
        assertEquals(0, CalculConge.moisComplets(entree, LocalDate.of(2026, 2, 14)));
        assertEquals(1, CalculConge.moisComplets(entree, LocalDate.of(2026, 2, 15)));
        assertEquals(12, CalculConge.moisComplets(entree, LocalDate.of(2027, 1, 15)));
        assertEquals(0, CalculConge.moisComplets(entree, LocalDate.of(2025, 12, 1)));
    }

    @Test
    void unAnDeServiceDonneTrenteJours() {
        assertEquals(0, new BigDecimal("30").compareTo(CalculConge.joursAcquis(12, new BigDecimal("2.5"))));
    }

    @Test
    void laFormuleDuCoursDonneLIndemniteDeConge() {
        // Conge = salaire brut x jours non pris / 30
        assertEquals(new BigDecimal("400000.00"),
                CalculConge.montantJours(new BigDecimal("1200000"), BigDecimal.TEN));
        assertEquals(new BigDecimal("50000.00"),
                CalculConge.montantJours(new BigDecimal("1200000"), new BigDecimal("1.25")));
    }
}
