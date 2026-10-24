package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.StatutAffectation;
import com.mrtovo.backend.bloc3.domain.TypeAffectation;

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
