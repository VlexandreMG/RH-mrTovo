package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.DemandeRecrutement;
import com.mrtovo.backend.entity.Departement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeRecrutementRepository extends JpaRepository<DemandeRecrutement, Long> {

    List<DemandeRecrutement> findByDepartementOrderByDateCreationDesc(Departement departement);

    List<DemandeRecrutement> findAllByOrderByDateCreationDesc();
}