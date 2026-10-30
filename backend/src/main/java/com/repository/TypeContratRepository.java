package com.mrtovo.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mrtovo.backend.entity.TypeContrat.TypeContrat;

@Repository
public interface TypeContratRepository extends JpaRepository<TypeContrat, Long> {
}