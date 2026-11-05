package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Utilisateur> findByEmployeId(Long employeId);
    List<Utilisateur> findByEmployeIsNotNull();
    boolean existsByRoleAndDepartementNomIgnoreCase(RoleUtilisateur role, String departementNom);
}
