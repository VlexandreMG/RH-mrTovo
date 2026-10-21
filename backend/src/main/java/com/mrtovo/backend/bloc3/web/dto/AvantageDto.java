package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.TypeAvantage;

import java.math.BigDecimal;

public record AvantageDto(
        Long id,
        TypeAvantage type,
        String libelle,
        String description,
        BigDecimal montant,
        Integer ordre) {
}
