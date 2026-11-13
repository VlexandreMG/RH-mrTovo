package com.mrtovo.backend.repository;

import com.mrtovo.backend.model.QuestionTest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionTestRepository extends JpaRepository<QuestionTest, Long> {

    @EntityGraph(attributePaths = "choix")
    List<QuestionTest> findByOffreEmploiIdOrderByOrdreAsc(Long offreId);

    long countByOffreEmploiId(Long offreId);
}
