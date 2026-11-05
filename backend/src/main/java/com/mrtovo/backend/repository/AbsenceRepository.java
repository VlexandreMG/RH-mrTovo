package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.Absence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    List<Absence> findByDateAbsenceBetween(LocalDate du, LocalDate au);

    Optional<Absence> findByEmployeIdAndDateAbsence(Long employeId, LocalDate dateAbsence);
}
