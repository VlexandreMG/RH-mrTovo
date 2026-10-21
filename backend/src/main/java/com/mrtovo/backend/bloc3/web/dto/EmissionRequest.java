package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.Acteur;

import java.time.LocalDate;

public record EmissionRequest(
        Acteur acteur,
        LocalDate dateLimiteValidite,
        String commentaire) {

    public String acteurOuDefaut(Acteur defaut) {
        return acteur != null ? acteur.name() : defaut.name();
    }
}
