package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

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
    private LocalDate dateEntretien;

    @ManyToOne
    @JoinColumn(name = "statut_entretien_id")
    private StatutEntretien statutEntretien;
}