package com.mrtovo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "affectation")
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEmbauche employe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poste_id", nullable = false)
    private PosteRecrutement poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private DepartementRecrutement departement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAffectation type = TypeAffectation.INITIALE;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAffectation statut = StatutAffectation.ACTIVE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EmployeEmbauche getEmploye() {
        return employe;
    }

    public void setEmploye(EmployeEmbauche employe) {
        this.employe = employe;
    }

    public PosteRecrutement getPoste() {
        return poste;
    }

    public void setPoste(PosteRecrutement poste) {
        this.poste = poste;
    }

    public DepartementRecrutement getDepartement() {
        return departement;
    }

    public void setDepartement(DepartementRecrutement departement) {
        this.departement = departement;
    }

    public TypeAffectation getType() {
        return type;
    }

    public void setType(TypeAffectation type) {
        this.type = type;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public StatutAffectation getStatut() {
        return statut;
    }

    public void setStatut(StatutAffectation statut) {
        this.statut = statut;
    }
}
