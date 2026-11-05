package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sert au manager comme au RH ; un refus doit etre justifie. */
public record DecisionCongeRequest(@NotNull Boolean accepte, @Size(max = 255) String commentaire) {
}
