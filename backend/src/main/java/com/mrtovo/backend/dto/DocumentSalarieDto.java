package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.EtatValidite;
import com.mrtovo.backend.entity.TypeDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentSalarieDto(
        Long id,
        Long employeId,
        TypeDocument typeDocument,
        String typeLibelle,
        String libelle,
        String numero,
        LocalDate dateEmission,
        LocalDate dateExpiration,
        EtatValidite etatValidite,
        Long joursAvantExpiration,
        boolean fichierDisponible,
        String nomFichier,
        LocalDateTime dateDepot,
        String deposePar) {
}
