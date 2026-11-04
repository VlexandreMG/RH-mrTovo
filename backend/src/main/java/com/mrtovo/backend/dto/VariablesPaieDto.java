package com.mrtovo.backend.dto;

import java.math.BigDecimal;

/** Ce que le temps de travail transmet a la paie et aux absences pour un salarie et un mois. */
public record VariablesPaieDto(
        Long employeId,
        String matricule,
        String nomComplet,
        int joursAbsents,
        int minutesPrevues,
        int minutesTravaillees,
        int minutesDues,
        int minutesCompensees,
        int minutesRetenues,
        int hsPalier1,
        int hsPalier2,
        int tauxPalier1,
        int tauxPalier2,
        int semainesAValider,
        int nbAnomalies,
        boolean provisoire,
        BigDecimal salaireBase,
        BigDecimal tauxHoraire,
        BigDecimal montantHeuresSup,
        BigDecimal montantRetenue) {
}
