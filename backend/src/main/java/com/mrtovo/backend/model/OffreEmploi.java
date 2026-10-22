package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "offre_emploi", schema = "mrtovo")
@Data
public class OffreEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_publication", nullable = false)
    private LocalDate datePublication;

    @ManyToOne
    @JoinColumn(name = "statut_offre_emploi_id")
    private StatutOffreEmploi statutOffreEmploi;
}