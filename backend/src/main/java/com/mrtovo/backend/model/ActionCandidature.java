package com.mrtovo.backend.model;

/** Ce qui peut arriver a une candidature. Chaque action ajoute une ligne a son historique. */
public enum ActionCandidature {

    CANDIDATURE_DEPOSEE("Candidature déposée"),
    PRESELECTION_RETENUE("Retenue à la présélection"),
    PRESELECTION_REJETEE("Rejetée à la présélection"),
    NOTE_SAISIE("Note du test saisie"),
    NOTE_CORRIGEE("Note du test corrigée"),
    TEST_VALIDE("Test validé"),
    TEST_ECHOUE("Test échoué"),
    ENTRETIEN_PLANIFIE("Entretien planifié"),
    ENTRETIEN_ANNULE("Entretien annulé"),
    CANDIDAT_ACCEPTE("Accepté après entretien"),
    CANDIDAT_REFUSE("Refusé après entretien");

    private final String libelle;

    ActionCandidature(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
