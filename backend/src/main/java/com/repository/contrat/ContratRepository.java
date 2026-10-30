package com.mrtovo.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mrtovo.backend.entity.contrat.Contrat;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, Long> {

    List<Contrat> findByEmployeId(Long employeId);

    List<Contrat> findByStatutContratId(Long statutContratId);

    List<Contrat> findByEmployeIdAndStatutContratId(Long employeId, Long statutContratId);
}