package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.Employe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeRepository extends JpaRepository<Employe, Long> {

    Optional<Employe> findByPropositionId(Long propositionId);

    Optional<Employe> findByCandidatId(Long candidatId);

    Optional<Employe> findByMatricule(String matricule);
}
