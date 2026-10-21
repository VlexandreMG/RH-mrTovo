package com.mrtovo.backend.bloc3.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GrilleDto(
        Long id,
        Long posteId,
        String posteTitre,
        String niveau,
        BigDecimal salaireMin,
        BigDecimal salaireMax,
        LocalDate dateDebut,
        LocalDate dateFin) {
}
