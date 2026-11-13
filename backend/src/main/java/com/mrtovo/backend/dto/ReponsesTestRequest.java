package com.mrtovo.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Les reponses du candidat : un choix par question. */
public record ReponsesTestRequest(@NotEmpty(message = "aucune réponse envoyée") List<@Valid Reponse> reponses) {

    public record Reponse(@NotNull Long questionId, @NotNull Long choixId) {}
}
