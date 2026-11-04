package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AffectationRegimeRequest(@NotNull Long regimeId, @NotNull LocalDate dateDebut) {
}
