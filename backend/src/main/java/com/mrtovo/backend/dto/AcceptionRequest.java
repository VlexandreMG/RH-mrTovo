package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.Acteur;

import java.time.LocalDate;

public record AcceptionRequest(
        Acteur acteur,
        String commentaire,
        LocalDate dateEmbauche) {

    public String acteurOuDefaut(Acteur defaut) {
        return acteur != null ? acteur.name() : defaut.name();
    }
}
