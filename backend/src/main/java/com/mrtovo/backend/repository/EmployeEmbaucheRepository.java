package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.EmployeEmbauche;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("bloc3EmployeRepository")
public interface EmployeEmbaucheRepository extends JpaRepository<EmployeEmbauche, Long> {

    Optional<EmployeEmbauche> findByPropositionId(Long propositionId);

    Optional<EmployeEmbauche> findByCandidatId(Long candidatId);

    Optional<EmployeEmbauche> findByMatricule(String matricule);

    Optional<EmployeEmbauche> findByEmailIgnoreCase(String email);

    /** Bloc 7 : deux demandes simultanees du meme salarie passent l'une apres l'autre (controle du solde). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from RecrutementEmploye e where e.id = :id")
    Optional<EmployeEmbauche> verrouiller(@Param("id") Long id);
}
