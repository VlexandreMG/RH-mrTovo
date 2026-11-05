package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IndemnisationRequest(
        @NotNull @Positive BigDecimal jours,
        @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "doit suivre le format AAAA-MM") String moisPaie,
        @NotBlank @Size(max = 255) String motif) {
}
