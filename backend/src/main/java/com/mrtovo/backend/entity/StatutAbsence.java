package com.mrtovo.backend.entity;

public enum StatutAbsence {

    JUSTIFIEE("Justifiée"),
    NON_JUSTIFIEE("Non justifiée");

    private final String libelle;

    StatutAbsence(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
