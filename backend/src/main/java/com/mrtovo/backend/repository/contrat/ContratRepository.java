package com.mrtovo.backend.repository.contrat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mrtovo.backend.entity.contrat.Contrat;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, Long> {
}