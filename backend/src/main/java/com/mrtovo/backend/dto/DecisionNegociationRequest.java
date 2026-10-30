package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.Acteur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record DecisionNegociationRequest(
        Acteur acteur,
        @NotBlank(message = "La decision est obligatoire") String decision,
        BigDecimal salaireAccorde,
        String commentaire,
        Boolean derogation,
        String motifDerogation,
        @Valid List<AvantageRequest> avantages) {

    public boolean derogationAccordee() {
        return Boolean.TRUE.equals(derogation);
    }

    public String acteurOuDefaut(Acteur defaut) {
        return acteur != null ? acteur.name() : defaut.name();
    }
}
