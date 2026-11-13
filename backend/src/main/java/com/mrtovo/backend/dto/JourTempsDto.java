package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.SourcePointage;
import com.mrtovo.backend.entity.StatutAbsence;

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
        SourcePointage pointageSource,
        boolean absent,
        String congeLibelle,
        Boolean congeRemunere,
        Long absenceId,
        StatutAbsence absenceStatut,
        int minutesPrevues,
        int minutesTravaillees,
        int minutesRetard,
        int minutesDepartAnticipe,
        int minutesHeuresSup,
        List<AnomaliePointageDto> anomalies) {
}
