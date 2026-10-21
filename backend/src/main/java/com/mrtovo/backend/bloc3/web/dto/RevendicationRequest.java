package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.Acteur;
import com.mrtovo.backend.bloc3.domain.TypeNegociation;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record RevendicationRequest(
        Acteur acteur,
        TypeNegociation type,
        BigDecimal salaireRevendique,
        String avantageRevendique,
        @NotBlank(message = "Le motif de la revendication est obligatoire") String motif) {

    public String acteurOuDefaut(Acteur defaut) {
        return acteur != null ? acteur.name() : defaut.name();
    }
}
