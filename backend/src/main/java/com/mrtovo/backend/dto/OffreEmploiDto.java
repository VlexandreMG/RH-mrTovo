package com.mrtovo.backend.dto;

import java.time.LocalDate;

public record OffreEmploiDto(
        Long id,
        String titre,
        String description,
        LocalDate datePublication,
        String statut) {
}
