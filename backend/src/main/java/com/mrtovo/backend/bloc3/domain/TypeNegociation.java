package com.mrtovo.backend.bloc3.domain;

public enum TypeNegociation {

    REVENDICATION("Revendication du candidat"),
    CONTRE_PROPOSITION("Contre-proposition de l'entreprise"),
    ACCORD_PARTIEL("Accord partiel");

    private final String libelle;

    TypeNegociation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
