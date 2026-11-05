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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mouvement de droit decide par le RH : jours en plus ou en moins sur le solde.
 * Les droits acquis chaque mois ne sont pas stockes : ils se deduisent de l'anciennete.
 */
@Entity
@Table(name = "droit_conge")
@Getter
@Setter
@NoArgsConstructor
public class DroitConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEmbauche employe;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_droit", nullable = false, length = 30)
    private TypeDroitConge typeDroit;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal jours;

    @Column(nullable = false)
    private String motif;

    /** Mois de paie (AAAA-MM) qui verse une indemnisation. */
    @Column(name = "mois_paie", length = 7)
    private String moisPaie;

    /** Montant fige au moment de la decision, meme si le salaire change ensuite. */
    @Column(precision = 14, scale = 2)
    private BigDecimal montant;

    @Column(name = "cree_par", nullable = false, length = 150)
    private String creePar;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
