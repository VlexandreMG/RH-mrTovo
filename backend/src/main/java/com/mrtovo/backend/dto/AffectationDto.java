package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.StatutAffectation;
import com.mrtovo.backend.entity.TypeAffectation;

import java.time.LocalDate;

public record AffectationDto(
        Long id,
        Long employeId,
        Long posteId,
        String posteTitre,
        Long departementId,
        String departementNom,
        TypeAffectation type,
        LocalDate dateDebut,
        LocalDate dateFin,
        String motif,
        StatutAffectation statut) {
}
