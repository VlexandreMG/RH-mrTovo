package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.StatutCandidatureRecrutement;
import com.mrtovo.backend.entity.StatutProposition;

import java.time.LocalDate;

public record CandidatureSelectionDto(
        Long id,
        Long candidatId,
        String nom,
        String prenom,
        String email,
        String telephone,
        Long offreEmploiId,
        String offreTitre,
        LocalDate dateCandidature,
        StatutCandidatureRecrutement statut,
        Long propositionId,
        StatutProposition propositionStatut) {
}
