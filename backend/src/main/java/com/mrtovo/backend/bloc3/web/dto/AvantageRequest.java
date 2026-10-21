package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.TypeAvantage;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AvantageRequest(
        TypeAvantage type,
        @NotBlank(message = "Le libelle de l'avantage est obligatoire") String libelle,
        String description,
        BigDecimal montant) {
}
