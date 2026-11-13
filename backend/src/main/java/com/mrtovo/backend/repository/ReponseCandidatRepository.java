package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.ReponseCandidat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReponseCandidatRepository extends JpaRepository<ReponseCandidat, Long> {

    /** Un candidat a-t-il deja repondu au questionnaire de cette offre ? Si oui, il est verrouille. */
    boolean existsByQuestionOffreEmploiId(Long offreId);

    List<ReponseCandidat> findByEvaluationIdOrderByQuestionOrdreAsc(Long evaluationId);

    long countByEvaluationId(Long evaluationId);

    long countByEvaluationIdAndChoixCorrectTrue(Long evaluationId);
}
