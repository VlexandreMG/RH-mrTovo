package com.mrtovo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Periode pendant laquelle un regime s'applique a un salarie (date_fin vide = en cours). */
@Entity
@Table(name = "planning_salarie")
@Getter
@Setter
@NoArgsConstructor
public class PlanningSalarie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEmbauche employe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regime_id", nullable = false)
    private RegimeTravail regime;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "affecte_par", nullable = false, length = 150)
    private String affectePar;

    public boolean couvre(LocalDate date) {
        return !date.isBefore(dateDebut) && (dateFin == null || !date.isAfter(dateFin));
    }
}
