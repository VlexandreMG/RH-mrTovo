package com.mrtovo.backend.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Erreurs prevues par les Blocs 1 et 2, traitees en priorite.
 * Le gestionnaire du Bloc 3 (RecrutementExceptionHandler) attrape Exception : sans cet ordre,
 * il passait avant et transformait ces erreurs en « Erreur interne » (500). Exemples :
 * « Identifiants invalides », « Le motif est obligatoire », « Saisissez la note du test ».
 * Les codes et les messages sont ceux d'origine.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErreursPrioritaires {

    /** Bloc 1 : regle metier non respectee. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> business(BusinessException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> identifiants(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Identifiants invalides"));
    }

    /** Bloc 2 : etape du recrutement non respectee. */
    @ExceptionHandler(RegleMetierException.class)
    public ResponseEntity<Map<String, String>> regleMetier(RegleMetierException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<Map<String, String>> introuvable(RessourceIntrouvableException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    /** Statut choisi par le code (compte deja existant, connexion candidat requise...). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> statutHttp(ResponseStatusException e) {
        String message = e.getReason() == null ? "Requête invalide." : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", message));
    }

    /** Parametres invalides du depot de candidature public (email, nom...). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> validationParametres(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " : " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
