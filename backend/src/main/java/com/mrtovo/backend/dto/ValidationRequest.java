package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.Acteur;

public record ValidationRequest(
        Acteur acteur,
        String commentaire,
        Boolean derogation,
        String motifDerogation) {

    public boolean derogationAccordee() {
        return Boolean.TRUE.equals(derogation);
    }

    public String acteurOuDefaut(Acteur defaut) {
        return acteur != null ? acteur.name() : defaut.name();
    }
}
