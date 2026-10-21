package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.Contrat;
import com.mrtovo.backend.bloc3.domain.StatutContrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    List<Contrat> findByEmployeIdOrderByDateDebutAsc(Long employeId);

    List<Contrat> findAllByOrderByDateDebutDesc();

    long countByStatutContrat(StatutContrat statut);
}
