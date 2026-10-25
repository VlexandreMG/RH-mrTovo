package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.Acteur;

public record ActionRequest(
        Acteur acteur,
        String commentaire) {

    public String acteurOuDefaut(Acteur defaut) {
        return acteur != null ? acteur.name() : defaut.name();
    }
}
