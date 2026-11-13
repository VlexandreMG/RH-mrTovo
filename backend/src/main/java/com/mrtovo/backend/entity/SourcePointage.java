package com.mrtovo.backend.entity;

/** Qui a produit le pointage : le salarie lui-meme, ou le RH qui ajoute un oubli ou corrige. */
public enum SourcePointage {

    SALARIE("Pointé par le salarié"),
    RH("Ajouté par le RH"),
    CORRIGE_RH("Corrigé par le RH");

    private final String libelle;

    SourcePointage(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
