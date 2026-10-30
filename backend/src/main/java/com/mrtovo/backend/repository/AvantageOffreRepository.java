package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.AvantageOffre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvantageOffreRepository extends JpaRepository<AvantageOffre, Long> {

    List<AvantageOffre> findByPropositionIdOrderByOrdreAsc(Long propositionId);
}
