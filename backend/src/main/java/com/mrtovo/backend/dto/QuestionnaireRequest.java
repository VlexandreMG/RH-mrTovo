package com.mrtovo.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Le questionnaire complet envoye par le RH : il remplace le precedent. */
public record QuestionnaireRequest(
        @NotEmpty(message = "ajoutez au moins une question")
        @Size(max = 50, message = "50 questions au maximum")
        List<@Valid Question> questions) {

    public record Question(
            @NotBlank(message = "l'énoncé est obligatoire") @Size(max = 2000) String enonce,
            @NotNull @Size(min = 2, max = 6, message = "chaque question doit avoir entre 2 et 6 choix")
            List<@Valid Choix> choix) {}

    public record Choix(
            @NotBlank(message = "chaque choix doit avoir un libellé") @Size(max = 500) String libelle,
            boolean correct) {}
}
