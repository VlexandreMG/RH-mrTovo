package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.DepartementRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("bloc3DepartementRepository")
public interface DepartementRecrutementRepository extends JpaRepository<DepartementRecrutement, Long> {
}
