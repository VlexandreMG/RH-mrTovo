package com.mrtovo.backend.repository.statutContrat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mrtovo.backend.StatutContrat.StatutContrat;

@Repository
public interface StatutContratRepository extends JpaRepository<StatutContrat, Long> {
}