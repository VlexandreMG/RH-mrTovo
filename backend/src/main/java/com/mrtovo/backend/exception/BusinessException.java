package com.mrtovo.backend.exception;

/** Erreur métier : renvoyée au client avec un message lisible (HTTP 400). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
