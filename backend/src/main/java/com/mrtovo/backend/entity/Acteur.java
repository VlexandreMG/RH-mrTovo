package com.mrtovo.backend.entity;

/**
 * Acteurs du workflow Bloc 3. L'authentification est hors perimetre :
 * l'acteur est transporte par l'API et trace dans l'historique.
 */
public enum Acteur {

    CANDIDAT,
    RECRUTEUR,
    RH,
    DIRECTION,
    MANAGER,
    SYSTEME
}
