package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "candidature", schema = "mrtovo")
@Data
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @ManyToOne
    @JoinColumn(name = "offre_emploi_id", nullable = false)
    private OffreEmploi offreEmploi;

    @Column(name = "date_candidature", nullable = false)
    private LocalDate dateCandidature;

    @ManyToOne
    @JoinColumn(name = "statut_candidature_id")
    private StatutCandidature statutCandidature;
}