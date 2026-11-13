package com.mrtovo.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Erreurs du parcours de recrutement (offres, candidatures, entretiens).
 * Le JSON illisible est traite dans {@link GlobalExceptionHandler}.
 * Les erreurs metier (409, 404, statuts choisis) sont dans {@link ErreursPrioritaires}, qui passe en priorite.
 */
@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " : " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
