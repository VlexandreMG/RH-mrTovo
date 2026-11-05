package com.mrtovo.backend.entity;

public enum TypeAnomaliePointage {

    POINTAGE_MANQUANT("Jour travaillé sans pointage"),
    SORTIE_MANQUANTE("Sortie non pointée"),
    TRAVAIL_JOUR_REPOS("Pointage un jour de repos"),
    RETARD_EXCESSIF("Retard important"),
    DEPART_ANTICIPE("Départ anticipé"),
    PRESENCE_EXCESSIVE("Présence anormalement longue"),
    SANS_REGIME("Aucun régime de travail affecté"),
    POINTAGE_PENDANT_CONGE("Pointage pendant un congé validé");

    private final String libelle;

    TypeAnomaliePointage(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
