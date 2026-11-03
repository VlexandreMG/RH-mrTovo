package com.mrtovo.backend.entity;

public enum ActionAudit {

    CONSULTATION_DOSSIER("Consultation du dossier"),
    MODIFICATION_IDENTITE("Modification de l'identité"),
    AJOUT_DOCUMENT("Ajout d'un document"),
    SUPPRESSION_DOCUMENT("Suppression d'un document"),
    TELECHARGEMENT_DOCUMENT("Téléchargement d'un document");

    private final String libelle;

    ActionAudit(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
