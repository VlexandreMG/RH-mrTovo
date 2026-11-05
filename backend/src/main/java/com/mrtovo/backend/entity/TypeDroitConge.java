package com.mrtovo.backend.entity;

public enum TypeDroitConge {

    AJUSTEMENT("Ajustement"),
    INDEMNISATION("Indemnisation");

    private final String libelle;

    TypeDroitConge(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
