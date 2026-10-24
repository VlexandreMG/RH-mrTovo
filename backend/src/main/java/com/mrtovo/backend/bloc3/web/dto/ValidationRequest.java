package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.Acteur;

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
