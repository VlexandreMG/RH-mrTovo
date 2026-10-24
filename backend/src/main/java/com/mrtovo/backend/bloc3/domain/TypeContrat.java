package com.mrtovo.backend.bloc3.domain;

public enum TypeContrat {

    CDI("Contrat à durée indéterminée"),
    CDD("Contrat à durée déterminée"),
    PERIODE_DESSAI("Contrat d'essai (CE)"),
    STAGE("Convention de stage"),
    ALTERNANCE("Contrat d'alternance");

    private final String libelle;

    TypeContrat(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean aDureeLimitee() {
        return this == CDD || this == PERIODE_DESSAI || this == STAGE || this == ALTERNANCE;
    }
}
