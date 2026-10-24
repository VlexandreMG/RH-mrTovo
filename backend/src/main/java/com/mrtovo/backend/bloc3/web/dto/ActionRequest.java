package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.Acteur;

public record ActionRequest(
        Acteur acteur,
        String commentaire) {

    public String acteurOuDefaut(Acteur defaut) {
        return acteur != null ? acteur.name() : defaut.name();
    }
}
