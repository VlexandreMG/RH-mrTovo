package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.Utilisateur;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    /** Serialise la creation du dossier salarie d'un compte (deux appels concurrents au premier chargement). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where lower(u.email) = lower(:email)")
    Optional<Utilisateur> verrouillerParEmail(@Param("email") String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Utilisateur> findByEmployeId(Long employeId);
    List<Utilisateur> findByEmployeIsNotNull();
    boolean existsByRoleAndDepartementNomIgnoreCase(RoleUtilisateur role, String departementNom);
}
