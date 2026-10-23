package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.Poste;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosteRepository extends JpaRepository<Poste, Long> {
}
