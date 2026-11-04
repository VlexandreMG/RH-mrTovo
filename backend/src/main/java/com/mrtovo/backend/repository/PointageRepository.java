package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PointageRepository extends JpaRepository<Pointage, Long> {

    List<Pointage> findByDatePointageBetween(LocalDate du, LocalDate au);

    Optional<Pointage> findByEmployeIdAndDatePointage(Long employeId, LocalDate datePointage);
}
