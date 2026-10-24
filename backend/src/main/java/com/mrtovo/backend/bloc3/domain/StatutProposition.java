package com.mrtovo.backend.bloc3.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'une proposition d'embauche (Bloc 3).
 *
 * BROUILLON -> VALIDEE -> EMISE -> (EN_NEGOCIATION -> REVISE -> VALIDEE -> EMISE) -> ACCEPTEE
 */
public enum StatutProposition {

    BROUILLON("Brouillon"),
    VALIDEE("Validée par les RH"),
    EMISE("Émise au candidat"),
    EN_NEGOCIATION("En négociation"),
    REVISE("Révisée après négociation"),
    ACCEPTEE("Acceptée par le candidat"),
    REFUSEE("Refusée"),
    EXPIREE("Expirée"),
    ANNULEE("Annulée"),
    SUPERSEDEE("Remplacée par une version ultérieure");

    private final String libelle;

    StatutProposition(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public Set<StatutProposition> transitionsAutorisees() {
        return switch (this) {
            case BROUILLON -> EnumSet.of(VALIDEE, ANNULEE);
            case VALIDEE -> EnumSet.of(EMISE, ANNULEE);
            case EMISE -> EnumSet.of(EN_NEGOCIATION, ACCEPTEE, REFUSEE, EXPIREE, ANNULEE);
            case EN_NEGOCIATION -> EnumSet.of(REVISE, SUPERSEDEE, REFUSEE, ANNULEE);
            case REVISE -> EnumSet.of(VALIDEE, ANNULEE, REFUSEE);
            case ACCEPTEE, REFUSEE, EXPIREE, ANNULEE, SUPERSEDEE -> EnumSet.noneOf(StatutProposition.class);
        };
    }

    public boolean peutEvoluerVers(StatutProposition cible) {
        return transitionsAutorisees().contains(cible);
    }

    public boolean estTerminal() {
        return transitionsAutorisees().isEmpty();
    }
}
