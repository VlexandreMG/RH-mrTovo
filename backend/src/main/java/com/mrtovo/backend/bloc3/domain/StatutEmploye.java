package com.mrtovo.backend.bloc3.domain;

public enum StatutEmploye {

    EN_PERIODE_DESSAI("En période d'essai"),
    ACTIF("Actif"),
    SUSPENDU("Suspendu"),
    SORTI("Sorti des effectifs");

    private final String libelle;

    StatutEmploye(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
