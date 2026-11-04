package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/** Toute correction doit etre justifiee : le motif part dans le journal d'audit. */
public record CorrectionPointageRequest(
        @NotNull LocalTime heureEntree,
        LocalTime heureSortie,
        @Size(max = 255) String commentaire,
        @NotBlank @Size(max = 255) String motif) {
}
