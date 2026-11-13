package com.mrtovo.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Le salarie active lui-meme son compte : son matricule ET l'email de son dossier doivent correspondre. */
public record ActivationCompteSalarieRequest(
        @NotBlank @Size(max = 50) String matricule,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String motDePasse) {
}
