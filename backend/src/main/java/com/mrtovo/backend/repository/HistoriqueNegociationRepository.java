package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.HistoriqueNegociation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueNegociationRepository extends JpaRepository<HistoriqueNegociation, Long> {

    List<HistoriqueNegociation> findByNegociationIdOrderByDateChangementAsc(Long negociationId);
}
