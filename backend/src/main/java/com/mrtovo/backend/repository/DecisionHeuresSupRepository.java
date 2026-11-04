package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.DecisionHeuresSup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DecisionHeuresSupRepository extends JpaRepository<DecisionHeuresSup, Long> {

    List<DecisionHeuresSup> findBySemaineDebutBetween(LocalDate du, LocalDate au);

    Optional<DecisionHeuresSup> findByEmployeIdAndSemaineDebut(Long employeId, LocalDate semaineDebut);

    List<DecisionHeuresSup> findByEmployeIdAndSemaineDebutGreaterThanEqual(Long employeId, LocalDate semaineDebut);
}
