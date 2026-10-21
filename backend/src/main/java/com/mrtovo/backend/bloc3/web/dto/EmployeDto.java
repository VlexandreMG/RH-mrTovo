package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.StatutEmploye;

import java.time.LocalDate;
import java.util.List;

public record EmployeDto(
        Long id,
        String matricule,
        Long candidatId,
        Long propositionId,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        Long posteId,
        String posteTitre,
        Long departementId,
        String departementNom,
        LocalDate dateEmbauche,
        LocalDate dateEntree,
        LocalDate dateFinContrat,
        LocalDate dateFinPeriodeEssai,
        StatutEmploye statut,
        List<ContratDto> contrats,
        List<AffectationDto> affectations) {
}
