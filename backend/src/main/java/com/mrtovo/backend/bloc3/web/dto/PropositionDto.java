package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.StatutProposition;
import com.mrtovo.backend.bloc3.domain.TypeContrat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropositionDto(
        Long id,
        Long candidatureId,
        String candidatNom,
        String candidatPrenom,
        String candidatEmail,
        String candidatTelephone,
        Long offreEmploiId,
        String offreTitre,
        Integer version,
        Long propositionParentId,
        BigDecimal salaireBase,
        String tempsTravail,
        String emploiDuTemps,
        String lieuTravail,
        LocalDate datePrisePoste,
        LocalDate dateLimiteValidite,
        LocalDate dateEmission,
        LocalDate dateFinContrat,
        TypeContrat typeContrat,
        Integer periodeEssaiMois,
        Long posteId,
        String posteTitre,
        Long departementId,
        String departementNom,
        StatutProposition statut,
        String motifRevision,
        String valideePar,
        LocalDate dateValidation,
        LocalDate dateAcceptation,
        List<AvantageDto> avantages,
        List<NegociationDto> negociations,
        List<HistoriqueDto> historique,
        GrilleDto grille,
        Boolean dansLaGrille,
        Long employeId,
        String matricule) {
}
