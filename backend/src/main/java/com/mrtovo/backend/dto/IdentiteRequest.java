package com.mrtovo.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Corps de PUT /api/bloc5/dossiers/{id}/identite : identite et coordonnees modifiables par le RH. */
public record IdentiteRequest(
        @NotBlank @Size(max = 255) String nom,
        @NotBlank @Size(max = 255) String prenom,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 50) String telephone,
        @Size(max = 255) String adresse,
        @Past LocalDate dateNaissance,
        @Size(max = 150) String lieuNaissance,
        @Pattern(regexp = "M|F", message = "doit valoir M ou F") String sexe,
        @Size(max = 100) String nationalite,
        @Size(max = 50) String numeroCin,
        @Size(max = 50) String numeroCnaps,
        @Pattern(regexp = "CELIBATAIRE|MARIE|DIVORCE|VEUF|UNION_LIBRE",
                message = "valeur de situation familiale inconnue") String situationFamiliale,
        @Min(0) Integer nombreEnfants,
        @Size(max = 255) String contactUrgenceNom,
        @Size(max = 50) String contactUrgenceTelephone) {
}
