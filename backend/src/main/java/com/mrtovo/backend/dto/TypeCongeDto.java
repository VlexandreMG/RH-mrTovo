package com.mrtovo.backend.dto;

public record TypeCongeDto(
        Long id,
        String code,
        String libelle,
        boolean decompteSolde,
        boolean remunere,
        boolean justificatifRequis,
        Integer plafondAnnuelJours) {
}
