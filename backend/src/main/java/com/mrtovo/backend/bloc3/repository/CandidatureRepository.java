package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.Candidature;
import com.mrtovo.backend.bloc3.domain.StatutCandidature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    List<Candidature> findByStatutInOrderByDateCandidatureDesc(Collection<StatutCandidature> statuts);

    List<Candidature> findByOffreEmploiId(Long offreEmploiId);

    long countByStatut(StatutCandidature statut);
}
