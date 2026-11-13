package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Une ligne de l'historique d'une candidature : qui a fait quoi, et quand.
 * Une ligne n'est jamais modifiee : @Immutable cote Hibernate, regle SQL cote base.
 */
@Entity
@Immutable
@Table(name = "historique_candidature", schema = "mrtovo")
@Getter
@Setter
public class HistoriqueCandidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private ActionCandidature action;

    /** Vides quand l'action ne change pas le statut (note corrigee, entretien annule...). */
    @ManyToOne
    @JoinColumn(name = "ancien_statut_id")
    private StatutCandidature ancienStatut;

    @ManyToOne
    @JoinColumn(name = "nouveau_statut_id")
    private StatutCandidature nouveauStatut;

    @Column(name = "auteur", nullable = false)
    private String auteur;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;
}
