package com.mrtovo.backend.dto;

import java.time.LocalDate;

public record PlanningSalarieDto(
        Long id,
        Long regimeId,
        String regimeLibelle,
        LocalDate dateDebut,
        LocalDate dateFin,
        String affectePar) {
}
