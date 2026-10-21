package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.HistoriqueProposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriquePropositionRepository extends JpaRepository<HistoriqueProposition, Long> {

    List<HistoriqueProposition> findByPropositionIdOrderByDateChangementAsc(Long propositionId);
}
