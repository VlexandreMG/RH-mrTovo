package com.mrtovo.backend.entity;

import java.util.EnumSet;
import java.util.Set;

public enum StatutDemandeConge {

    EN_ATTENTE_MANAGER("En attente du manager"),
    EN_ATTENTE_RH("En attente du RH"),
    VALIDEE("Validée"),
    REFUSEE("Refusée"),
    ANNULEE("Annulée");

    /** Une demande dans l'un de ces etats occupe ses dates et son solde. */
    public static final Set<StatutDemandeConge> ACTIFS = EnumSet.of(EN_ATTENTE_MANAGER, EN_ATTENTE_RH, VALIDEE);

    private final String libelle;

    StatutDemandeConge(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean enAttente() {
        return this == EN_ATTENTE_MANAGER || this == EN_ATTENTE_RH;
    }
}
