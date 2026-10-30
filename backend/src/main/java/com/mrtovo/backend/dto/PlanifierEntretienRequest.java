package com.mrtovo.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record PlanifierEntretienRequest(
        @NotNull(message = "la date est obligatoire")
        @Future(message = "doit être dans le futur")
        LocalDateTime date,
        String lieu) {}
