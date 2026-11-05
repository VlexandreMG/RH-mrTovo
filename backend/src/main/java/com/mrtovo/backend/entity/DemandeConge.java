package com.mrtovo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_conge")
@Getter
@Setter
@NoArgsConstructor
public class DemandeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEmbauche employe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_conge_id", nullable = false)
    private TypeConge typeConge;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "nb_jours", nullable = false)
    private int nbJours;

    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutDemandeConge statut;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande = LocalDateTime.now();

    @Column(name = "demande_par", nullable = false, length = 150)
    private String demandePar;

    @Column(name = "avis_manager_par", length = 150)
    private String avisManagerPar;

    @Column(name = "date_avis_manager")
    private LocalDateTime dateAvisManager;

    @Column(name = "commentaire_manager")
    private String commentaireManager;

    @Column(name = "decide_par", length = 150)
    private String decidePar;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @Column(name = "commentaire_rh")
    private String commentaireRh;

    @Column(name = "justificatif_cle", length = 100)
    private String justificatifCle;

    @Column(name = "justificatif_nom")
    private String justificatifNom;

    @Column(name = "justificatif_type", length = 100)
    private String justificatifType;

    public boolean couvre(LocalDate date) {
        return !date.isBefore(dateDebut) && !date.isAfter(dateFin);
    }
}
