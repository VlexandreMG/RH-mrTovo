package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.TypeContrat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropositionCreateRequest(
        @NotNull(message = "La candidature est obligatoire") Long candidatureId,
        Long posteId,
        Long departementId,
        @NotNull(message = "Le salaire de base est obligatoire")
        @DecimalMin(value = "0.0", message = "Le salaire ne peut pas etre negatif") BigDecimal salaireBase,
        @NotBlank(message = "Le temps de travail est obligatoire") String tempsTravail,
        String emploiDuTemps,
        @NotBlank(message = "Le lieu de travail est obligatoire") String lieuTravail,
        @NotNull(message = "La date de prise de poste est obligatoire") LocalDate datePrisePoste,
        LocalDate dateLimiteValidite,
        LocalDate dateFinContrat,
        TypeContrat typeContrat,
        Integer periodeEssaiMois,
        List<AvantageRequest> avantages,
        String motif) {
}
