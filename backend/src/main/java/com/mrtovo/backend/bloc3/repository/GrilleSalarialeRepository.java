package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.GrilleSalariale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrilleSalarialeRepository extends JpaRepository<GrilleSalariale, Long> {

    List<GrilleSalariale> findByPosteIdOrderBySalaireMinAsc(Long posteId);
}
