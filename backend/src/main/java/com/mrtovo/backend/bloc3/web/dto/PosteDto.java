package com.mrtovo.backend.bloc3.web.dto;

public record PosteDto(
        Long id,
        String titre,
        String description,
        Long departementId,
        String departementNom,
        String statut) {
}
