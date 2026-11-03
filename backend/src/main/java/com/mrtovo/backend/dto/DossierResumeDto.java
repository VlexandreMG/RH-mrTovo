package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.StatutEmploye;

import java.time.LocalDate;

/** Ligne de la liste des dossiers : l'essentiel + les compteurs d'alertes. */
public record DossierResumeDto(
        Long id,
        String matricule,
        String nom,
        String prenom,
        String email,
        String posteTitre,
        String departementNom,
        LocalDate dateEntree,
        StatutEmploye statut,
        int nbDocuments,
        int nbDocumentsExpires,
        int nbDocumentsBientotExpires,
        int nbAlertes) {
}
