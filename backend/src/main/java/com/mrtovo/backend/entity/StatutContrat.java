package com.mrtovo.backend.entity;

public enum StatutContrat {

    A_SIGNER("À signer"),
    SIGNE("Signé"),
    EN_PERIODE_DESSAI("En période d'essai"),
    ACTIF("Actif"),
    TERMINE("Terminé"),
    RUPTURE("Rompu");

    private final String libelle;

    StatutContrat(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
