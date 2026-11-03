package com.mrtovo.backend.entity;

public enum TypeDocument {

    CIN("Carte d'identité nationale"),
    DIPLOME("Diplôme"),
    CERTIFICAT_RESIDENCE("Certificat de résidence"),
    CASIER_JUDICIAIRE("Bulletin de casier judiciaire"),
    RIB("Relevé d'identité bancaire"),
    CONTRAT_SIGNE("Contrat signé"),
    CERTIFICAT_MEDICAL("Certificat médical d'aptitude"),
    AUTRE("Autre document");

    private final String libelle;

    TypeDocument(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
