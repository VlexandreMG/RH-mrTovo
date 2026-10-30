package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.TypeAvantage;

import java.math.BigDecimal;

public record AvantageDto(
        Long id,
        TypeAvantage type,
        String libelle,
        String description,
        BigDecimal montant,
        Integer ordre) {
}
