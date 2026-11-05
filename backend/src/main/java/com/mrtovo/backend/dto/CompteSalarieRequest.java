package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompteSalarieRequest(@NotBlank @Size(min = 8, max = 72) String motDePasse) {
}
