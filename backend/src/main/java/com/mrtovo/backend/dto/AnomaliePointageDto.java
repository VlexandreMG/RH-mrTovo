package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.TypeAnomaliePointage;

import java.time.LocalDate;

public record AnomaliePointageDto(
        Long employeId,
        String matricule,
        String nomComplet,
        LocalDate date,
        TypeAnomaliePointage type,
        String libelle,
        String detail) {
}
