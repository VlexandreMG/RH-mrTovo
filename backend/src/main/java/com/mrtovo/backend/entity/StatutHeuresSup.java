package com.mrtovo.backend.entity;

public enum StatutHeuresSup {

    SANS_HS("Aucune heure supplémentaire"),
    A_VALIDER("À valider"),
    VALIDEE("Validée"),
    REFUSEE("Refusée");

    private final String libelle;

    StatutHeuresSup(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
