package com.mrtovo.backend.dto;

import java.util.List;

/** Le QCM d'une offre vu par le RH, bonnes reponses comprises. verrouille : un candidat a deja repondu. */
public record QuestionnaireDTO(Long offreId, String offreTitre, boolean verrouille, List<Question> questions) {

    public record Question(Long id, String enonce, List<Choix> choix) {}

    public record Choix(Long id, String libelle, boolean correct) {}
}
