package com.mrtovo.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Une ligne de la semaine : ce qui etait prevu, ce qui a ete pointe, ce qui en est deduit. */
public record JourTempsDto(
        LocalDate date,
        LocalTime heureDebutPrevue,
        LocalTime heureFinPrevue,
        Long pointageId,
        LocalTime heureEntree,
        LocalTime heureSortie,
        String commentaire,
        boolean absent,
        int minutesPrevues,
        int minutesTravaillees,
        int minutesRetard,
        int minutesDepartAnticipe,
        int minutesHeuresSup,
        List<AnomaliePointageDto> anomalies) {
}
