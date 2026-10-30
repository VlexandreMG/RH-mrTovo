package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByCandidatureId(Long candidatureId);
}
