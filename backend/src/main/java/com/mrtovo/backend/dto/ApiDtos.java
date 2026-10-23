package com.mrtovo.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Objets échangés avec le frontend (on n'expose jamais directement les entités JPA). */
public final class ApiDtos {

    private ApiDtos() {
    }

    // ---------- Requêtes ----------

    public record LigneRequest(Long idNiveau, Integer effectif) {
    }

    public record DemandeRecrutementRequest(
            Long idPoste,
            String typeBesoin,
            String motif,
            Long idEmployeARemplacer,
            BigDecimal budgetDemandeTotal,
            LocalDate dateSouhaitee,
            List<LigneRequest> lignes
    ) {
    }

    /**
     * Estimation du RH pour une ligne.
     * salaireMin / salaireMax ne sont renseignés que pour une CREATION qui définit une nouvelle fourchette.
     */
    public record EstimationRequest(
            Long idLigne,
            BigDecimal coutEstimeParPersonne,
            BigDecimal salaireMin,
            BigDecimal salaireMax
    ) {
    }

    public record ValidationRequest(
            String decision,
            String commentaire,
            List<EstimationRequest> estimations
    ) {
    }

    // ---------- Réponses ----------

    public record Ref(Long id, String libelle) {
    }

    public record EmployeDto(Long idEmploye, String nom, String matricule,
                             Long idPoste, String poste, String niveau, String departement) {
    }

    public record GrilleDto(Long idGrille, Long idPoste, String poste, Long idNiveau, String niveau,
                            BigDecimal salaireMin, BigDecimal salaireMax,
                            LocalDate dateDebut, LocalDate dateFin) {
    }

    public record LigneDto(Long idLigne, Long idNiveau, String niveau, Integer effectif,
                           Long idGrille, BigDecimal salaireMin, BigDecimal salaireMax,
                           BigDecimal coutEstimeParPersonne, BigDecimal coutEstimeTotal) {
    }

    public record ValidationDto(Long idValidation, String etape, String decision, String commentaire,
                                String validateur, String roleValidateur, LocalDateTime dateValidation) {
    }

    public record DemandeDto(
            Long idDemande,
            Long idPoste, String poste,
            Long idDepartement, String departement,
            String manager,
            String typeBesoin, String motif,
            String employeARemplacer,
            BigDecimal budgetDemandeTotal,
            Integer effectifTotal,
            BigDecimal coutEstimeTotal,
            LocalDate dateSouhaitee,
            String statut,
            LocalDateTime dateCreation,
            LocalDateTime dateOuverture,
            List<LigneDto> lignes,
            List<ValidationDto> validations
    ) {
    }
}
