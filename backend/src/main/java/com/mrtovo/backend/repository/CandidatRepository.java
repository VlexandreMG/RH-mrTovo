package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.Candidat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {
    Optional<Candidat> findByEmailIgnoreCase(String email);
}
