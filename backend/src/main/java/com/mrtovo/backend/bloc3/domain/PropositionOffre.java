package com.mrtovo.backend.bloc3.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proposition_offre")
public class PropositionOffre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Column(nullable = false)
    private Integer version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposition_parent_id")
    private PropositionOffre propositionParent;

    @Column(name = "salaire_base", nullable = false)
    private BigDecimal salaireBase;

    @Column(name = "temps_travail", nullable = false)
    private String tempsTravail;

    @Column(name = "emploi_du_temps")
    private String emploiDuTemps;

    @Column(name = "lieu_travail", nullable = false)
    private String lieuTravail;

    @Column(name = "date_prise_poste", nullable = false)
    private LocalDate datePrisePoste;

    @Column(name = "date_limite_validite", nullable = false)
    private LocalDate dateLimiteValidite;

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission;

    @Column(name = "date_fin_contrat")
    private LocalDate dateFinContrat;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false)
    private TypeContrat typeContrat = TypeContrat.CDI;

    @Column(name = "periode_essai_mois", nullable = false)
    private Integer periodeEssaiMois = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private Departement departement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutProposition statut = StatutProposition.BROUILLON;

    @Column(name = "motif_revision")
    private String motifRevision;

    @Column(name = "validee_par")
    private String valideePar;

    @Column(name = "date_validation")
    private LocalDate dateValidation;

    @Column(name = "date_acceptation")
    private LocalDate dateAcceptation;

    @OneToMany(mappedBy = "proposition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC, id ASC")
    private List<AvantageOffre> avantages = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Candidature getCandidature() {
        return candidature;
    }

    public void setCandidature(Candidature candidature) {
        this.candidature = candidature;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public PropositionOffre getPropositionParent() {
        return propositionParent;
    }

    public void setPropositionParent(PropositionOffre propositionParent) {
        this.propositionParent = propositionParent;
    }

    public BigDecimal getSalaireBase() {
        return salaireBase;
    }

    public void setSalaireBase(BigDecimal salaireBase) {
        this.salaireBase = salaireBase;
    }

    public String getTempsTravail() {
        return tempsTravail;
    }

    public void setTempsTravail(String tempsTravail) {
        this.tempsTravail = tempsTravail;
    }

    public String getEmploiDuTemps() {
        return emploiDuTemps;
    }

    public void setEmploiDuTemps(String emploiDuTemps) {
        this.emploiDuTemps = emploiDuTemps;
    }

    public String getLieuTravail() {
        return lieuTravail;
    }

    public void setLieuTravail(String lieuTravail) {
        this.lieuTravail = lieuTravail;
    }

    public LocalDate getDatePrisePoste() {
        return datePrisePoste;
    }

    public void setDatePrisePoste(LocalDate datePrisePoste) {
        this.datePrisePoste = datePrisePoste;
    }

    public LocalDate getDateLimiteValidite() {
        return dateLimiteValidite;
    }

    public void setDateLimiteValidite(LocalDate dateLimiteValidite) {
        this.dateLimiteValidite = dateLimiteValidite;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getDateFinContrat() {
        return dateFinContrat;
    }

    public void setDateFinContrat(LocalDate dateFinContrat) {
        this.dateFinContrat = dateFinContrat;
    }

    public TypeContrat getTypeContrat() {
        return typeContrat;
    }

    public void setTypeContrat(TypeContrat typeContrat) {
        this.typeContrat = typeContrat;
    }

    public Integer getPeriodeEssaiMois() {
        return periodeEssaiMois;
    }

    public void setPeriodeEssaiMois(Integer periodeEssaiMois) {
        this.periodeEssaiMois = periodeEssaiMois;
    }

    public Poste getPoste() {
        return poste;
    }

    public void setPoste(Poste poste) {
        this.poste = poste;
    }

    public Departement getDepartement() {
        return departement;
    }

    public void setDepartement(Departement departement) {
        this.departement = departement;
    }

    public StatutProposition getStatut() {
        return statut;
    }

    public void setStatut(StatutProposition statut) {
        this.statut = statut;
    }

    public String getMotifRevision() {
        return motifRevision;
    }

    public void setMotifRevision(String motifRevision) {
        this.motifRevision = motifRevision;
    }

    public String getValideePar() {
        return valideePar;
    }

    public void setValideePar(String valideePar) {
        this.valideePar = valideePar;
    }

    public LocalDate getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(LocalDate dateValidation) {
        this.dateValidation = dateValidation;
    }

    public LocalDate getDateAcceptation() {
        return dateAcceptation;
    }

    public void setDateAcceptation(LocalDate dateAcceptation) {
        this.dateAcceptation = dateAcceptation;
    }

    public List<AvantageOffre> getAvantages() {
        return avantages;
    }

    public void setAvantages(List<AvantageOffre> avantages) {
        this.avantages = avantages;
    }

    public void addAvantage(AvantageOffre avantage) {
        avantage.setProposition(this);
        this.avantages.add(avantage);
    }
}
