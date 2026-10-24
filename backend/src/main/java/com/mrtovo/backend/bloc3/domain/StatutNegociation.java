package com.mrtovo.backend.bloc3.domain;

import java.util.EnumSet;
import java.util.Set;

public enum StatutNegociation {

    EN_ATTENTE("En attente d'analyse"),
    EN_ANALYSE("En analyse RH"),
    CONTRE_PROPOSITION("Contre-proposition envoyée"),
    ACCEPTE("Acceptée"),
    REFUSE("Refusée");

    private final String libelle;

    StatutNegociation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public Set<StatutNegociation> transitionsAutorisees() {
        return switch (this) {
            case EN_ATTENTE -> EnumSet.of(EN_ANALYSE, ACCEPTE, REFUSE);
            case EN_ANALYSE -> EnumSet.of(CONTRE_PROPOSITION, ACCEPTE, REFUSE);
            case CONTRE_PROPOSITION -> EnumSet.of(EN_ANALYSE, ACCEPTE, REFUSE);
            case ACCEPTE, REFUSE -> EnumSet.noneOf(StatutNegociation.class);
        };
    }

    public boolean peutEvoluerVers(StatutNegociation cible) {
        return transitionsAutorisees().contains(cible);
    }
}
