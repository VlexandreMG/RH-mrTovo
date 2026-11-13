package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.HistoriqueCandidature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueCandidatureRepository extends JpaRepository<HistoriqueCandidature, Long> {

    /** Ordre chronologique ; l'identifiant departage deux actions de la meme seconde. */
    List<HistoriqueCandidature> findByCandidatureIdOrderByDateActionAscIdAsc(Long candidatureId);
}
