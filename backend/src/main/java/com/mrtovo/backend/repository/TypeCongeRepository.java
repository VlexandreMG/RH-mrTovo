package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.TypeConge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TypeCongeRepository extends JpaRepository<TypeConge, Long> {

    List<TypeConge> findAllByOrderByLibelleAsc();

    Optional<TypeConge> findByCodeIgnoreCase(String code);
}
