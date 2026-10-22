package com.mrtovo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fourchette salariale mensuelle de référence pour un poste à un niveau donné, avec période de validité. */
@Entity
@Table(name = "grille_salariale", uniqueConstraints = {
        @UniqueConstraint(name = "uk_grille_poste_niveau_debut", columnNames = {"id_poste", "id_niveau", "date_debut"})
})
@Getter
@Setter
@NoArgsConstructor
public class GrilleSalariale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grille")
    private Long idGrille;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poste", nullable = false)
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_niveau", nullable = false)
    private NiveauPoste niveau;

    @Column(name = "salaire_min", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMin;

    @Column(name = "salaire_max", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMax;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;
}
