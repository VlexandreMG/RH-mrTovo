package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.TypeContrat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Vue complete du dossier administratif d'un salarie (referentiel central du SIRH). */
public record DossierSalarieDto(
        Long id,
        String matricule,
        Identite identite,
        SituationProfessionnelle situation,
        OrigineRecrutement origine,
        List<ContratDto> contrats,
        List<AffectationDto> affectations,
        List<EvenementCarriereDto> carriere,
        List<DocumentSalarieDto> documents,
        List<String> alertes) {

    public record Identite(
            String nom,
            String prenom,
            String email,
            String telephone,
            String adresse,
            LocalDate dateNaissance,
            Integer age,
            String lieuNaissance,
            String sexe,
            String nationalite,
            String numeroCin,
            String numeroCnaps,
            String situationFamiliale,
            Integer nombreEnfants,
            String contactUrgenceNom,
            String contactUrgenceTelephone) {
    }

    public record SituationProfessionnelle(
            String posteTitre,
            String departementNom,
            StatutEmploye statut,
            String statutLibelle,
            LocalDate dateEmbauche,
            LocalDate dateEntree,
            LocalDate dateFinPeriodeEssai,
            LocalDate dateFinContrat,
            String anciennete,
            String contratEnCoursReference,
            TypeContrat contratEnCoursType,
            BigDecimal salaireActuel) {
    }

    public record OrigineRecrutement(
            Long candidatId,
            Long candidatureId,
            String offreTitre,
            LocalDate dateCandidature,
            Long propositionId,
            LocalDate dateAcceptation) {
    }
}
