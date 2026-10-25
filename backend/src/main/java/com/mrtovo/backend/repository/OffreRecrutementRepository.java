package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.OffreRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("bloc3OffreEmploiRepository")
public interface OffreRecrutementRepository extends JpaRepository<OffreRecrutement, Long> {
}
