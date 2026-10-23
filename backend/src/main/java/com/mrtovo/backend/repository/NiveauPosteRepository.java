package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.NiveauPoste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NiveauPosteRepository extends JpaRepository<NiveauPoste, Long> {
    List<NiveauPoste> findAllByOrderByRangAsc();
}
