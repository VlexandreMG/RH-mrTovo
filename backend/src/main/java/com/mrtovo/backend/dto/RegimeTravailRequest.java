package com.mrtovo.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

public record RegimeTravailRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 255) String libelle,
        @Min(0) @Max(60) int toleranceRetardMinutes,
        @NotEmpty @Valid List<Horaire> horaires) {

    public record Horaire(
            @Min(1) @Max(7) int jourSemaine,
            @NotNull LocalTime heureDebut,
            @NotNull LocalTime heureFin,
            @Min(0) int pauseMinutes) {
    }
}
