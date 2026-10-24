package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AffectationRepository extends JpaRepository<Affectation, Long> {

    List<Affectation> findByEmployeIdOrderByDateDebutAsc(Long employeId);
}
