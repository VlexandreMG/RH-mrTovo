package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.PropositionOffre;
import com.mrtovo.backend.bloc3.domain.StatutProposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropositionOffreRepository extends JpaRepository<PropositionOffre, Long> {

    List<PropositionOffre> findByCandidatureIdOrderByVersionAsc(Long candidatureId);

    List<PropositionOffre> findByStatutOrderByDateEmissionDesc(StatutProposition statut);

    List<PropositionOffre> findAllByOrderByDateEmissionDesc();

    long countByStatut(StatutProposition statut);

    @Query("select coalesce(max(p.version), 0) from PropositionOffre p where p.candidature.id = :candidatureId")
    int maxVersionPourCandidature(@Param("candidatureId") Long candidatureId);
}
