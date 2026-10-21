package com.mrtovo.backend.bloc3.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_negociation")
public class HistoriqueNegociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negociation_id", nullable = false)
    private Negociation negociation;

    @Column(name = "ancien_statut")
    private String ancienStatut;

    @Column(name = "nouveau_statut", nullable = false)
    private String nouveauStatut;

    @Column(nullable = false)
    private String action;

    private String auteur;

    private String commentaire;

    @Column(name = "date_changement", nullable = false)
    private LocalDateTime dateChangement = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Negociation getNegociation() {
        return negociation;
    }

    public void setNegociation(Negociation negociation) {
        this.negociation = negociation;
    }

    public String getAncienStatut() {
        return ancienStatut;
    }

    public void setAncienStatut(String ancienStatut) {
        this.ancienStatut = ancienStatut;
    }

    public String getNouveauStatut() {
        return nouveauStatut;
    }

    public void setNouveauStatut(String nouveauStatut) {
        this.nouveauStatut = nouveauStatut;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDateTime getDateChangement() {
        return dateChangement;
    }

    public void setDateChangement(LocalDateTime dateChangement) {
        this.dateChangement = dateChangement;
    }
}
