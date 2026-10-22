package com.mrtovo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Chaque avis est une ligne distincte : l'historique n'est jamais écrasé. */
@Entity
@Table(name = "validation")
@Getter
@Setter
@NoArgsConstructor
public class Validation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_validation")
    private Long idValidation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_demande", nullable = false)
    private DemandeRecrutement demande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_validateur", nullable = false)
    private Utilisateur validateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtapeValidation etape;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisionValidation decision;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_validation", nullable = false)
    private LocalDateTime dateValidation = LocalDateTime.now();
}
