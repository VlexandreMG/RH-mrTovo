package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "evaluation", schema = "mrtovo")
@Data
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Column(name = "note_test")
    private Integer noteTest;

    @ManyToOne
    @JoinColumn(name = "statut_evaluation_id")
    private StatutEvaluation statutEvaluation;
}