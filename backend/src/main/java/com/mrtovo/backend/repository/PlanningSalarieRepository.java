package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.PlanningSalarie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PlanningSalarieRepository extends JpaRepository<PlanningSalarie, Long> {

    /** Toutes les periodes qui touchent l'intervalle [du, au], regimes et horaires compris. */
    @EntityGraph(attributePaths = {"regime", "regime.horaires"})
    @Query("""
            select p from PlanningSalarie p
            where p.dateDebut <= :au and (p.dateFin is null or p.dateFin >= :du)
            order by p.dateDebut
            """)
    List<PlanningSalarie> chevauchant(@Param("du") LocalDate du, @Param("au") LocalDate au);

    @EntityGraph(attributePaths = "regime")
    List<PlanningSalarie> findByEmployeIdOrderByDateDebutAsc(Long employeId);
}
