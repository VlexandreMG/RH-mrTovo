package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.GrilleSalarialeRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("bloc3GrilleSalarialeRepository")
public interface GrilleRecrutementRepository extends JpaRepository<GrilleSalarialeRecrutement, Long> {

    List<GrilleSalarialeRecrutement> findByPosteIdOrderBySalaireMinAsc(Long posteId);
}
