package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.Negociation;
import com.mrtovo.backend.entity.StatutNegociation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NegociationRepository extends JpaRepository<Negociation, Long> {

    List<Negociation> findByPropositionIdOrderByDateDemandeDesc(Long propositionId);

    List<Negociation> findAllByOrderByDateDemandeDesc();

    long countByStatutIn(Collection<StatutNegociation> statuts);
}
