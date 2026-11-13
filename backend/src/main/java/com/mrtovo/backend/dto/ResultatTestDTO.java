package com.mrtovo.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Le test d'un candidat vu par le RH : la note et le detail de chaque reponse. */
public record ResultatTestDTO(Long candidatureId, LocalDateTime dateSoumission, BigDecimal note,
                              long nbBonnes, long nbQuestions, List<Ligne> reponses) {

    public record Ligne(String enonce, String reponseDonnee, String bonneReponse, boolean correcte) {}
}
