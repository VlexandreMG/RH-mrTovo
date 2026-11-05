package com.mrtovo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnulationCongeRequest(@NotBlank @Size(max = 255) String commentaire) {
}
