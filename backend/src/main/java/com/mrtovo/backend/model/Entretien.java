package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "entretien", schema = "mrtovo")
@Data
public class Entretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @Column(name = "date_entretien", nullable = false)
    private LocalDateTime dateEntretien;

    private String lieu;

    /** Compte-rendu saisi par le recruteur après l'entretien. */
    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "statut_entretien_id")
    private StatutEntretien statutEntretien;
}
