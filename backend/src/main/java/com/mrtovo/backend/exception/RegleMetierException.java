package com.mrtovo.backend.exception;

/** Action impossible à l'étape actuelle du parcours (renvoie HTTP 409). */
public class RegleMetierException extends RuntimeException {
    public RegleMetierException(String message) { super(message); }
}
