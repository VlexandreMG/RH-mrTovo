package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.Entretien;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EntretienRepository extends JpaRepository<Entretien, Long> {
    /** Le dernier entretien (planifié, terminé ou annulé) d'une évaluation. */
    Optional<Entretien> findFirstByEvaluationIdOrderByIdDesc(Long evaluationId);
    boolean existsByEvaluationIdAndStatutEntretienId(Long evaluationId, Long statutId);
}
