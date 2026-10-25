package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.CandidatureSelection;
import com.mrtovo.backend.entity.StatutCandidatureRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository("bloc3CandidatureRepository")
public interface CandidatureSelectionRepository extends JpaRepository<CandidatureSelection, Long> {

    List<CandidatureSelection> findByStatutCandidatureIdInOrderByDateCandidatureDesc(Collection<Long> statuts);

    List<CandidatureSelection> findByOffreEmploiId(Long offreEmploiId);

    long countByStatutCandidatureId(Long statutId);
}
