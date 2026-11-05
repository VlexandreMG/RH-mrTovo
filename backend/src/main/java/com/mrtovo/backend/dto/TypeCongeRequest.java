package com.mrtovo.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TypeCongeRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 255) String libelle,
        boolean decompteSolde,
        boolean remunere,
        boolean justificatifRequis,
        @Min(1) Integer plafondAnnuelJours) {
}
