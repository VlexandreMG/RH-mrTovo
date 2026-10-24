package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.Acteur;

import java.time.LocalDate;

public record AcceptionRequest(
        Acteur acteur,
        String commentaire,
        LocalDate dateEmbauche) {

    public String acteurOuDefaut(Acteur defaut) {
        return acteur != null ? acteur.name() : defaut.name();
    }
}
