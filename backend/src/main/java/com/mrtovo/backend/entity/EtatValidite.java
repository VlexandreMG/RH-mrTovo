package com.mrtovo.backend.entity;

import java.time.LocalDate;

/**
 * Etat de validite d'un document administratif. Il n'est jamais stocke :
 * il depend de la date du jour et est recalcule a chaque lecture.
 */
public enum EtatValidite {

    PERMANENT("Sans date d'expiration"),
    VALIDE("Valide"),
    EXPIRE_BIENTOT("Expire bientôt"),
    EXPIRE("Expiré");

    /** Nombre de jours avant l'expiration a partir duquel le RH est alerte. */
    public static final int SEUIL_ALERTE_JOURS = 30;

    private final String libelle;

    EtatValidite(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static EtatValidite calculer(LocalDate dateExpiration, LocalDate aujourdhui) {
        if (dateExpiration == null) {
            return PERMANENT;
        }
        if (dateExpiration.isBefore(aujourdhui)) {
            return EXPIRE;
        }
        if (!dateExpiration.isAfter(aujourdhui.plusDays(SEUIL_ALERTE_JOURS))) {
            return EXPIRE_BIENTOT;
        }
        return VALIDE;
    }

    public boolean demandeAttention() {
        return this == EXPIRE || this == EXPIRE_BIENTOT;
    }
}
