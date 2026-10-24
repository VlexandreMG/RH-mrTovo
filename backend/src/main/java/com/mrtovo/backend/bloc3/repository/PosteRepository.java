package com.mrtovo.backend.bloc3.repository;

import com.mrtovo.backend.bloc3.domain.Poste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosteRepository extends JpaRepository<Poste, Long> {

    List<Poste> findByDepartementId(Long departementId);
}
