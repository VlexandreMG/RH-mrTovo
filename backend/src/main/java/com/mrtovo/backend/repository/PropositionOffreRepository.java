package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.PropositionOffre;
import com.mrtovo.backend.entity.StatutProposition;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropositionOffreRepository extends JpaRepository<PropositionOffre, Long> {

    List<PropositionOffre> findByCandidatureIdOrderByVersionAsc(Long candidatureId);

    List<PropositionOffre> findByStatutOrderByDateEmissionDesc(StatutProposition statut);

    List<PropositionOffre> findAllByOrderByDateEmissionDesc();

    List<PropositionOffre> findByCandidature_Candidat_IdOrderByDateEmissionDesc(Long candidatId);

    Optional<PropositionOffre> findByIdAndCandidature_Candidat_Id(Long id, Long candidatId);

    long countByStatut(StatutProposition statut);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PropositionOffre p where p.id = :id")
    Optional<PropositionOffre> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(max(p.version), 0) from PropositionOffre p where p.candidature.id = :candidatureId")
    int maxVersionPourCandidature(@Param("candidatureId") Long candidatureId);
}
