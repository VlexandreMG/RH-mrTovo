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

/** Decision du RH sur un jour absent. Le jour absent lui-meme vient du calcul du Bloc 6. */
@Entity
@Table(name = "absence")
@Getter
@Setter
@NoArgsConstructor
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEmbauche employe;

    @Column(name = "date_absence", nullable = false)
    private LocalDate dateAbsence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAbsence statut;

    private String motif;

    @Column(name = "justificatif_cle", length = 100)
    private String justificatifCle;

    @Column(name = "justificatif_nom")
    private String justificatifNom;

    @Column(name = "justificatif_type", length = 100)
    private String justificatifType;

    @Column(name = "qualifie_par", nullable = false, length = 150)
    private String qualifiePar;

    @Column(name = "date_qualification", nullable = false)
    private LocalDateTime dateQualification = LocalDateTime.now();
}
