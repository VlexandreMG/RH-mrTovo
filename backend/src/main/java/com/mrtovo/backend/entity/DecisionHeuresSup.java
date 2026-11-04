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
@Table(name = "decision_heures_sup")
@Getter
@Setter
@NoArgsConstructor
public class DecisionHeuresSup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEmbauche employe;

    @Column(name = "semaine_debut", nullable = false)
    private LocalDate semaineDebut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutHeuresSup statut;

    @Column(name = "minutes_brutes", nullable = false)
    private int minutesBrutes;

    @Column(name = "minutes_validees", nullable = false)
    private int minutesValidees;

    private String commentaire;

    @Column(name = "decide_par", nullable = false, length = 150)
    private String decidePar;

    @Column(name = "date_decision", nullable = false)
    private LocalDateTime dateDecision = LocalDateTime.now();
}
