package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.EmployeEmbauche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("bloc3EmployeRepository")
public interface EmployeEmbaucheRepository extends JpaRepository<EmployeEmbauche, Long> {

    Optional<EmployeEmbauche> findByPropositionId(Long propositionId);

    Optional<EmployeEmbauche> findByCandidatId(Long candidatId);

    Optional<EmployeEmbauche> findByMatricule(String matricule);

    Optional<EmployeEmbauche> findByEmailIgnoreCase(String email);
}
