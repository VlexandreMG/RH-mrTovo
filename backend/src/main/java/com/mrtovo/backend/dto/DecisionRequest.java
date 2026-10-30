package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotNull;

/** Décision du recruteur : valide = true (retenir / valider / accepter) ou false (rejeter). */
public record DecisionRequest(@NotNull Boolean valide, String commentaire) {}
