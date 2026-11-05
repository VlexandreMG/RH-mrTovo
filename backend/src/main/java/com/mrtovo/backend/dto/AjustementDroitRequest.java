package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** jours est signe : positif pour ajouter au solde, negatif pour en retirer. */
public record AjustementDroitRequest(@NotNull BigDecimal jours, @NotBlank @Size(max = 255) String motif) {
}
