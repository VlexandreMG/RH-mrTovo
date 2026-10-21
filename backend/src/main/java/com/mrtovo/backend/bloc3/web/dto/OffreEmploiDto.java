package com.mrtovo.backend.bloc3.web.dto;

import java.time.LocalDate;

public record OffreEmploiDto(
        Long id,
        String titre,
        String description,
        LocalDate datePublication,
        String statut) {
}
