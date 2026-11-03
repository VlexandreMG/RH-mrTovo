package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.DocumentSalarie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentSalarieRepository extends JpaRepository<DocumentSalarie, Long> {

    List<DocumentSalarie> findByEmployeIdOrderByDateDepotDesc(Long employeId);
}
