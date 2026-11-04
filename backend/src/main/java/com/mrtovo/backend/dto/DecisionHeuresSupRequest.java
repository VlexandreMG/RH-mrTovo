package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.StatutHeuresSup;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** decision vaut VALIDEE ou REFUSEE ; minutesValidees peut etre inferieur aux heures sup constatees. */
public record DecisionHeuresSupRequest(
        @NotNull Long employeId,
        @NotNull LocalDate semaineDebut,
        @NotNull StatutHeuresSup decision,
        @Min(0) int minutesValidees,
        @Size(max = 255) String commentaire) {
}
