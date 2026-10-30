package com.mrtovo.backend.entity.TypeContrat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "typeContrat")
public class TypeContrat{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false)
    private Long id;
    @Column(name = "libelle",nullable = false)
    private String libelle;

    public TypeContrat() {
    }
    public TypeContrat(Long id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }
    public TypeContrat(String libelle) {
        this.libelle = libelle;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getLibelle() {
        return libelle;
    }
    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}