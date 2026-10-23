package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    List<Candidature> findByOffreEmploiIdOrderByDateCandidatureAscIdAsc(Long offreEmploiId);
    List<Candidature> findByStatutCandidatureIdOrderByIdAsc(Long statutId);
    long countByOffreEmploiId(Long offreEmploiId);
}
