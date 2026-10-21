package com.mrtovo.backend.bloc3.domain;

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

import java.math.BigDecimal;

@Entity
@Table(name = "avantage_offre")
public class AvantageOffre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposition_id", nullable = false)
    private PropositionOffre proposition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAvantage type;

    @Column(nullable = false)
    private String libelle;

    private String description;

    private BigDecimal montant;

    @Column(nullable = false)
    private Integer ordre = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PropositionOffre getProposition() {
        return proposition;
    }

    public void setProposition(PropositionOffre proposition) {
        this.proposition = proposition;
    }

    public TypeAvantage getType() {
        return type;
    }

    public void setType(TypeAvantage type) {
        this.type = type;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Integer getOrdre() {
        return ordre;
    }

    public void setOrdre(Integer ordre) {
        this.ordre = ordre;
    }
}
