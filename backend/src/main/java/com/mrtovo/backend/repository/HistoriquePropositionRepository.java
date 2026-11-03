package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.HistoriqueProposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriquePropositionRepository extends JpaRepository<HistoriqueProposition, Long> {

    List<HistoriqueProposition> findByPropositionIdOrderByDateChangementAsc(Long propositionId);
}
