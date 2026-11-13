package com.mrtovo.backend.dto;

import java.time.LocalDateTime;

/** Une ligne de l'historique telle que l'affiche la fiche du candidat. */
public record HistoriqueCandidatureDTO(
        Long id,
        String action,
        String actionLibelle,
        Long ancienStatutId,
        String ancienStatut,
        Long nouveauStatutId,
        String nouveauStatut,
        String auteur,
        String details,
        LocalDateTime date
) {
}
