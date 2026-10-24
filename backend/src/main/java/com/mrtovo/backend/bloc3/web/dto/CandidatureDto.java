package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.StatutCandidature;
import com.mrtovo.backend.bloc3.domain.StatutProposition;

import java.time.LocalDate;

public record CandidatureDto(
        Long id,
        Long candidatId,
        String nom,
        String prenom,
        String email,
        String telephone,
        Long offreEmploiId,
        String offreTitre,
        LocalDate dateCandidature,
        StatutCandidature statut,
        Long propositionId,
        StatutProposition propositionStatut) {
}
