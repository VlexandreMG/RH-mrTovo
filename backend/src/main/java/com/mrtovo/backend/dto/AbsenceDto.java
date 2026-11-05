package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.StatutAbsence;

import java.time.LocalDate;

/** id et statut restent vides tant que le RH n'a pas qualifie le jour. */
public record AbsenceDto(
        Long id,
        Long employeId,
        String matricule,
        String nomComplet,
        LocalDate date,
        StatutAbsence statut,
        String motif,
        boolean justificatifDisponible,
        String justificatifNom,
        String qualifiePar) {
}
