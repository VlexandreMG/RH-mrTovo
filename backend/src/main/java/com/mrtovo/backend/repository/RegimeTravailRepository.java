package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.RegimeTravail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegimeTravailRepository extends JpaRepository<RegimeTravail, Long> {

    @EntityGraph(attributePaths = "horaires")
    List<RegimeTravail> findAllByOrderByLibelleAsc();

    Optional<RegimeTravail> findByCodeIgnoreCase(String code);
}
