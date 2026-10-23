package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.OffreEmploi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Long> {
    Optional<OffreEmploi> findByDemandeIdDemande(Long idDemande);
}
