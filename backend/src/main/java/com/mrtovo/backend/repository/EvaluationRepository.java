package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.Evaluation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByCandidatureId(Long candidatureId);

    /** Verrou pendant l'envoi des reponses : deux envois simultanes ne donnent qu'une seule tentative. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evaluation e where e.candidature.id = :candidatureId")
    Optional<Evaluation> verrouillerParCandidature(@Param("candidatureId") Long candidatureId);
}
