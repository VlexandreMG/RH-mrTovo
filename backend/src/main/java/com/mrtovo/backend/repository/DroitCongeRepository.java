package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.DroitConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DroitCongeRepository extends JpaRepository<DroitConge, Long> {

    List<DroitConge> findByEmployeIdOrderByDateCreationDesc(Long employeId);

    List<DroitConge> findByMoisPaie(String moisPaie);
}
