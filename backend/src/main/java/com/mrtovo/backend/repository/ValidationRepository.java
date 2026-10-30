package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.Validation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationRepository extends JpaRepository<Validation, Long> {
}
