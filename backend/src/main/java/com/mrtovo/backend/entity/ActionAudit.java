package com.mrtovo.backend.entity;

public enum ActionAudit {

    CONSULTATION_DOSSIER("Consultation du dossier"),
    MODIFICATION_IDENTITE("Modification de l'identité"),
    AJOUT_DOCUMENT("Ajout d'un document"),
    SUPPRESSION_DOCUMENT("Suppression d'un document"),
    TELECHARGEMENT_DOCUMENT("Téléchargement d'un document"),
    AFFECTATION_REGIME("Affectation d'un régime de travail"),
    SAISIE_POINTAGE("Saisie d'un pointage"),
    POINTAGE_ARRIVEE("Arrivée pointée par le salarié"),
    POINTAGE_DEPART("Départ pointé par le salarié"),
    CORRECTION_POINTAGE("Correction d'un pointage"),
    SUPPRESSION_POINTAGE("Suppression d'un pointage"),
    DECISION_HEURES_SUP("Décision sur les heures supplémentaires"),
    REOUVERTURE_HEURES_SUP("Réouverture des heures supplémentaires"),
    CREATION_COMPTE_SALARIE("Création du compte salarié"),
    DEMANDE_CONGE("Demande de congé"),
    AVIS_MANAGER_CONGE("Avis du manager sur un congé"),
    DECISION_RH_CONGE("Décision du RH sur un congé"),
    ANNULATION_CONGE("Annulation d'un congé"),
    AJOUT_JUSTIFICATIF("Ajout d'un justificatif"),
    QUALIFICATION_ABSENCE("Qualification d'une absence"),
    AJUSTEMENT_DROIT_CONGE("Ajustement du droit à congé"),
    INDEMNISATION_CONGE("Indemnisation de congés non pris");

    private final String libelle;

    ActionAudit(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
