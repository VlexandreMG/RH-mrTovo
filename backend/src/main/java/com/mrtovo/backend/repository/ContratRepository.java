package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.Contrat;
import com.mrtovo.backend.entity.StatutContrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    List<Contrat> findByEmployeIdOrderByDateDebutAsc(Long employeId);

    List<Contrat> findAllByOrderByDateDebutDesc();

    long countByStatutContrat(StatutContrat statut);
}
