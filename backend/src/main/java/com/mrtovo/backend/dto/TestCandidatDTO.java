package com.mrtovo.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ce que voit le candidat. Aucun champ ne porte la bonne reponse ni la note :
 * le RH decide, le candidat apprend la suite par le parcours de recrutement.
 */
public final class TestCandidatDTO {

    private TestCandidatDTO() {}

    /**
     * etat : A_PASSER, EN_PREPARATION (pas encore de questionnaire), ENVOYE (en attente de la decision du RH),
     * EVALUE (note manuelle), REUSSI (test valide), REFUSE (candidature refusee au test), TERMINE (autre cas).
     */
    public record Resume(Long candidatureId, String offreTitre, String etat, int nbQuestions,
                         LocalDateTime dateSoumission) {}

    public record Questionnaire(Long candidatureId, String offreTitre, List<Question> questions) {}

    public record Question(Long id, String enonce, List<Choix> choix) {}

    public record Choix(Long id, String libelle) {}
}
