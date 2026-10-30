package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.GrilleSalariale;
import com.mrtovo.backend.entity.NiveauPoste;
import com.mrtovo.backend.entity.Poste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrilleSalarialeRepository extends JpaRepository<GrilleSalariale, Long> {
    List<GrilleSalariale> findByPosteAndNiveauOrderByDateDebutDesc(Poste poste, NiveauPoste niveau);

    List<GrilleSalariale> findAllByOrderByPosteIntituleAscNiveauRangAscDateDebutDesc();
}
