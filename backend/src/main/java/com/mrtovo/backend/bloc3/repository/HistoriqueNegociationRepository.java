package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.HistoriqueNegociation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueNegociationRepository extends JpaRepository<HistoriqueNegociation, Long> {

    List<HistoriqueNegociation> findByNegociationIdOrderByDateChangementAsc(Long negociationId);
}
