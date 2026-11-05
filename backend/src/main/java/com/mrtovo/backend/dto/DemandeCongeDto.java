package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.StatutDemandeConge;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DemandeCongeDto(
        Long id,
        Long employeId,
        String matricule,
        String nomComplet,
        String departementNom,
        Long typeCongeId,
        String typeLibelle,
        boolean decompteSolde,
        boolean justificatifRequis,
        LocalDate dateDebut,
        LocalDate dateFin,
        int nbJours,
        String motif,
        StatutDemandeConge statut,
        String statutLibelle,
        LocalDateTime dateDemande,
        String avisManagerPar,
        String commentaireManager,
        String decidePar,
        String commentaireRh,
        boolean justificatifDisponible,
        String justificatifNom,
        BigDecimal soldeDisponible) {
}
