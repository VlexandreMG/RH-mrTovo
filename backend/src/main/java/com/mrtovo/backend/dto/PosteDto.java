package com.mrtovo.backend.dto;

public record PosteDto(
        Long id,
        String titre,
        String description,
        Long departementId,
        String departementNom,
        String statut) {
}
