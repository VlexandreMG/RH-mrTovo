package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Saisie d'un pointage : une seule ligne par salarie et par jour. */
public record PointageRequest(
        @NotNull Long employeId,
        @NotNull LocalDate date,
        @NotNull LocalTime heureEntree,
        LocalTime heureSortie,
        @Size(max = 255) String commentaire) {
}
