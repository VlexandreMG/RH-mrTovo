package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.Candidat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {

    Optional<Candidat> findByEmailIgnoreCase(String email);
}
