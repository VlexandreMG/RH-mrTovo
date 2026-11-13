package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Ajout d'un pointage par le RH (oubli, panne...) : une seule ligne par salarie et par jour.
 * Le motif est obligatoire : d'ordinaire, c'est le salarie qui pointe lui-meme.
 */
public record PointageRequest(
        @NotNull Long employeId,
        @NotNull LocalDate date,
        @NotNull LocalTime heureEntree,
        LocalTime heureSortie,
        @Size(max = 255) String commentaire,
        @Size(max = 255) String motif) {
}
