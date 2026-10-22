package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.Acteur;
import com.mrtovo.backend.bloc3.domain.TypeNegociation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RevendicationRequest(
        Acteur acteur,
        TypeNegociation type,
        @Positive(message = "Le salaire revendique doit etre positif") BigDecimal salaireRevendique,
        String avantageRevendique,
        @NotBlank(message = "Le motif de la revendication est obligatoire")
        @Size(max = 255, message = "Le motif ne doit pas depasser 255 caracteres") String motif) {

    public String acteurOuDefaut(Acteur defaut) {
        return acteur != null ? acteur.name() : defaut.name();
    }
}
