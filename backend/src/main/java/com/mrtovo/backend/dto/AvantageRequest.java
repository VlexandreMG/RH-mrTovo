package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.TypeAvantage;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AvantageRequest(
        TypeAvantage type,
        @NotBlank(message = "Le libelle de l'avantage est obligatoire") String libelle,
        String description,
        BigDecimal montant) {
}
