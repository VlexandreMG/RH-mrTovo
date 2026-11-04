package com.mrtovo.backend.entity;

public enum ActionAudit {

    CONSULTATION_DOSSIER("Consultation du dossier"),
    MODIFICATION_IDENTITE("Modification de l'identité"),
    AJOUT_DOCUMENT("Ajout d'un document"),
    SUPPRESSION_DOCUMENT("Suppression d'un document"),
    TELECHARGEMENT_DOCUMENT("Téléchargement d'un document"),
    AFFECTATION_REGIME("Affectation d'un régime de travail"),
    SAISIE_POINTAGE("Saisie d'un pointage"),
    CORRECTION_POINTAGE("Correction d'un pointage"),
    SUPPRESSION_POINTAGE("Suppression d'un pointage"),
    DECISION_HEURES_SUP("Décision sur les heures supplémentaires"),
    REOUVERTURE_HEURES_SUP("Réouverture des heures supplémentaires");

    private final String libelle;

    ActionAudit(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
