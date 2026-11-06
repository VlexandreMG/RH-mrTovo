package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.DemandeConge;
import com.mrtovo.backend.entity.StatutDemandeConge;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {

    @EntityGraph(attributePaths = "typeConge")
    List<DemandeConge> findByEmployeIdOrderByDateDebutDesc(Long employeId);

    @EntityGraph(attributePaths = {"typeConge", "employe"})
    List<DemandeConge> findAllByOrderByDateDemandeDesc();

    /** Demandes d'un salarie dans ces etats dont les dates touchent [debut, fin]. */
    @Query("""
            select d from DemandeConge d
            where d.employe.id = :employeId and d.statut in :statuts
              and d.dateDebut <= :fin and d.dateFin >= :debut
            """)
    List<DemandeConge> chevauchantes(@Param("employeId") Long employeId,
                                     @Param("debut") LocalDate debut,
                                     @Param("fin") LocalDate fin,
                                     @Param("statuts") Collection<StatutDemandeConge> statuts);

    /** Tous les conges valides qui touchent la periode : ils alimentent le planning du Bloc 6. */
    @EntityGraph(attributePaths = "typeConge")
    @Query("""
            select d from DemandeConge d
            where d.statut = com.mrtovo.backend.entity.StatutDemandeConge.VALIDEE
              and d.dateDebut <= :au and d.dateFin >= :du
            """)
    List<DemandeConge> validesSurPeriode(@Param("du") LocalDate du, @Param("au") LocalDate au);

    /** Toutes les demandes dans ces etats qui touchent la periode : elles alimentent le calendrier. */
    @EntityGraph(attributePaths = "typeConge")
    @Query("""
            select d from DemandeConge d
            where d.statut in :statuts
              and d.dateDebut <= :au and d.dateFin >= :du
            """)
    List<DemandeConge> surPeriode(@Param("du") LocalDate du,
                                  @Param("au") LocalDate au,
                                  @Param("statuts") Collection<StatutDemandeConge> statuts);
}
