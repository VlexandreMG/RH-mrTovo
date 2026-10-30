package com.mrtovo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;

@Entity(name = "RecrutementOffreEmploi")
@Table(name = "offre_emploi")
public class OffreRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String description;

    @Column(name = "date_publication", nullable = false)
    private LocalDate datePublication;

    @Column(name = "date_cloture")
    private LocalDate dateCloture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_offre_emploi_id")
    private com.mrtovo.backend.model.StatutOffreEmploi statutOffreEmploi;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDate datePublication) {
        this.datePublication = datePublication;
    }

    public LocalDate getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDate dateCloture) {
        this.dateCloture = dateCloture;
    }

    @Transient
    public StatutOffreRecrutement getStatut() {
        if (statutOffreEmploi == null || statutOffreEmploi.getId() == null) {
            return StatutOffreRecrutement.BROUILLON;
        }
        return switch (statutOffreEmploi.getId().intValue()) {
            case 1 -> StatutOffreRecrutement.PUBLIEE;
            case 2 -> StatutOffreRecrutement.CLOTUREE;
            default -> StatutOffreRecrutement.BROUILLON;
        };
    }

    @Transient
    public void setStatut(StatutOffreRecrutement statut) {
        if (statut == null) {
            statutOffreEmploi = null;
            return;
        }
        com.mrtovo.backend.model.StatutOffreEmploi reference =
                new com.mrtovo.backend.model.StatutOffreEmploi();
        reference.setId(switch (statut) {
            case PUBLIEE -> 1L;
            case CLOTUREE -> 2L;
            case BROUILLON, ARCHIVEE -> 3L;
        });
        statutOffreEmploi = reference;
    }
}
