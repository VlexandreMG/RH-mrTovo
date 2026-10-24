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
import java.time.LocalDate;

@Entity
@Table(name = "negociation")
public class Negociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposition_id", nullable = false)
    private PropositionOffre proposition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNegociation type = TypeNegociation.REVENDICATION;

    @Column(name = "salaire_revendique")
    private BigDecimal salaireRevendique;

    @Column(name = "avantage_revendique")
    private String avantageRevendique;

    @Column(nullable = false)
    private String motif;

    @Column(name = "date_demande", nullable = false)
    private LocalDate dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutNegociation statut = StatutNegociation.EN_ATTENTE;

    @Column(name = "decide_par")
    private String decidePar;

    @Column(name = "date_decision")
    private LocalDate dateDecision;

    @Column(name = "commentaire_decision")
    private String commentaireDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposition_resultante_id")
    private PropositionOffre propositionResultante;

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

    public Candidat getCandidat() {
        return candidat;
    }

    public void setCandidat(Candidat candidat) {
        this.candidat = candidat;
    }

    public TypeNegociation getType() {
        return type;
    }

    public void setType(TypeNegociation type) {
        this.type = type;
    }

    public BigDecimal getSalaireRevendique() {
        return salaireRevendique;
    }

    public void setSalaireRevendique(BigDecimal salaireRevendique) {
        this.salaireRevendique = salaireRevendique;
    }

    public String getAvantageRevendique() {
        return avantageRevendique;
    }

    public void setAvantageRevendique(String avantageRevendique) {
        this.avantageRevendique = avantageRevendique;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDate getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(LocalDate dateDemande) {
        this.dateDemande = dateDemande;
    }

    public StatutNegociation getStatut() {
        return statut;
    }

    public void setStatut(StatutNegociation statut) {
        this.statut = statut;
    }

    public String getDecidePar() {
        return decidePar;
    }

    public void setDecidePar(String decidePar) {
        this.decidePar = decidePar;
    }

    public LocalDate getDateDecision() {
        return dateDecision;
    }

    public void setDateDecision(LocalDate dateDecision) {
        this.dateDecision = dateDecision;
    }

    public String getCommentaireDecision() {
        return commentaireDecision;
    }

    public void setCommentaireDecision(String commentaireDecision) {
        this.commentaireDecision = commentaireDecision;
    }

    public PropositionOffre getPropositionResultante() {
        return propositionResultante;
    }

    public void setPropositionResultante(PropositionOffre propositionResultante) {
        this.propositionResultante = propositionResultante;
    }
}
