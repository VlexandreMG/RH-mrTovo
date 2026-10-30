package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.Departement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartementRepository extends JpaRepository<Departement, Long> {
}
