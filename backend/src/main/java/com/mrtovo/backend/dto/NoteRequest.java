package com.mrtovo.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record NoteRequest(
        @NotNull(message = "la note est obligatoire")
        @DecimalMin(value = "0", message = "doit être comprise entre 0 et 20")
        @DecimalMax(value = "20", message = "doit être comprise entre 0 et 20")
        BigDecimal note) {}
