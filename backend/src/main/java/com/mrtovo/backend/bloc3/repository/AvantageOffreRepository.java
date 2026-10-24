package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.AvantageOffre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvantageOffreRepository extends JpaRepository<AvantageOffre, Long> {

    List<AvantageOffre> findByPropositionIdOrderByOrdreAsc(Long propositionId);
}
