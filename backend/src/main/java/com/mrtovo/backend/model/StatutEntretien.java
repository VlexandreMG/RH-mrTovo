package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "statut_entretien", schema = "mrtovo")
@Data
public class StatutEntretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;
}