package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.Negociation;
import com.mrtovo.backend.bloc3.domain.StatutNegociation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NegociationRepository extends JpaRepository<Negociation, Long> {

    List<Negociation> findByPropositionIdOrderByDateDemandeDesc(Long propositionId);

    List<Negociation> findAllByOrderByDateDemandeDesc();

    long countByStatutIn(Collection<StatutNegociation> statuts);
}
