package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.StatutContrat;
import com.mrtovo.backend.bloc3.domain.TypeContrat;

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
