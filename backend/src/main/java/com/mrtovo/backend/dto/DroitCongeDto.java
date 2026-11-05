package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.TypeDroitConge;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DroitCongeDto(
        Long id,
        TypeDroitConge typeDroit,
        BigDecimal jours,
        String motif,
        String moisPaie,
        BigDecimal montant,
        String creePar,
        LocalDateTime dateCreation) {
}
