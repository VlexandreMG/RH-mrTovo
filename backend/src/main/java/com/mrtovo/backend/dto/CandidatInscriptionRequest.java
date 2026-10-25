package com.mrtovo.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CandidatInscriptionRequest(
        @NotBlank @Size(max = 255) String nom,
        @NotBlank @Size(max = 255) String prenom,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String motDePasse,
        @Size(max = 50) String telephone) {
}
