package com.mrtovo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demande_recrutement")
@Getter
@Setter
@NoArgsConstructor
public class DemandeRecrutement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande")
    private Long idDemande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poste", nullable = false)
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_departement", nullable = false)
    private Departement departement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_manager", nullable = false)
    private Utilisateur manager;

    /** CREATION ou REMPLACEMENT. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_besoin", nullable = false, length = 20)
    private TypeBesoin typeBesoin;

    /** Raison métier en texte libre (ex. : « Développer une base de données sécurisée pour le projet X »). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    /** Seulement pour un remplacement : l'employé qui part. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_employe_remplace")
    private Employe employeARemplacer;

    /** Enveloppe mensuelle globale demandée par le manager. */
    @Column(name = "budget_demande_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal budgetDemandeTotal;

    @Column(name = "date_souhaitee", nullable = false)
    private LocalDate dateSouhaitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutDemande statut = StatutDemande.EN_ATTENTE_RH;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_ouverture")
    private LocalDateTime dateOuverture;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idLigne ASC")
    private List<LigneDemande> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL)
    @OrderBy("dateValidation ASC, idValidation ASC")
    private List<Validation> validations = new ArrayList<>();
}
