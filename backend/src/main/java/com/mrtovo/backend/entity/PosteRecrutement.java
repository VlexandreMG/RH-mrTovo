package com.mrtovo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "RecrutementPoste")
@Table(name = "poste")
public class PosteRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    private String description;

    @ManyToOne
    @JoinColumn(name = "departement_id")
    private DepartementRecrutement departement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPoste statut = StatutPoste.ACTIF;

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

    public DepartementRecrutement getDepartement() {
        return departement;
    }

    public void setDepartement(DepartementRecrutement departement) {
        this.departement = departement;
    }

    public StatutPoste getStatut() {
        return statut;
    }

    public void setStatut(StatutPoste statut) {
        this.statut = statut;
    }
}
