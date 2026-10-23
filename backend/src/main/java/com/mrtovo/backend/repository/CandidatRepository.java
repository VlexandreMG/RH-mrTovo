package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.Candidat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {
}
