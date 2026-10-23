package com.mrtovo.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Une carte du Kanban : la candidature avec son test et son dernier entretien. */
public record CandidatureDTO(
        Long id,
        Long offreId,
        String offreTitre,
        Long candidatId,
        String nom,
        String prenom,
        String email,
        String telephone,
        String fichierCvUrl,
        LocalDate dateCandidature,
        Long statutId,
        String statut,
        TestDTO test,
        EntretienDTO entretien
) {
    public record TestDTO(Long id, BigDecimal note, Long statutId, String statut) {}

    public record EntretienDTO(Long id, LocalDateTime date, String lieu, String commentaire,
                               Long statutId, String statut) {}
}
