package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.StatutContrat;
import com.mrtovo.backend.entity.TypeContrat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratDto(
        Long id,
        Long employeId,
        String employeNom,
        String matricule,
        String reference,
        TypeContrat typeContrat,
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal salaire,
        StatutContrat statutContrat,
        String documentUrl) {
}
