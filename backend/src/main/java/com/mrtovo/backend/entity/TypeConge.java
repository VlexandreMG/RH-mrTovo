package com.mrtovo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "type_conge")
@Getter
@Setter
@NoArgsConstructor
public class TypeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String libelle;

    /** Vrai : les jours pris diminuent le solde. */
    @Column(name = "decompte_solde", nullable = false)
    private boolean decompteSolde;

    /** Faux : chaque jour pris est retenu sur la paie. */
    @Column(nullable = false)
    private boolean remunere = true;

    @Column(name = "justificatif_requis", nullable = false)
    private boolean justificatifRequis;

    @Column(name = "plafond_annuel_jours")
    private Integer plafondAnnuelJours;
}
