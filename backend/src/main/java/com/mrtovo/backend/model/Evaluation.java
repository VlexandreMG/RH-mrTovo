package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Le test passé par un candidat présélectionné. */
@Entity
@Table(name = "evaluation", schema = "mrtovo")
@Data
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "candidature_id", nullable = false, unique = true)
    private Candidature candidature;

    /** Note sur 20 : calculée à partir des réponses du candidat au QCM. */
    @Column(name = "note_test", precision = 4, scale = 2)
    private BigDecimal noteTest;

    @ManyToOne
    @JoinColumn(name = "statut_evaluation_id")
    private StatutEvaluation statutEvaluation;

    /** Moment où le candidat a envoyé ses réponses ; null tant qu'il n'a pas passé le test. */
    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;
}
