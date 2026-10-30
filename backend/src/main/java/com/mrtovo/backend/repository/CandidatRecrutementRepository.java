package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.CandidatRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("bloc3CandidatRepository")
public interface CandidatRecrutementRepository extends JpaRepository<CandidatRecrutement, Long> {

    Optional<CandidatRecrutement> findByEmailIgnoreCase(String email);
}
