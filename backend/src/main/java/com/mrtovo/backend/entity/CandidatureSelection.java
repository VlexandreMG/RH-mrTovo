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

@Entity(name = "RecrutementCandidature")
@Table(name = "candidature")
public class CandidatureSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false)
    private CandidatRecrutement candidat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_emploi_id", nullable = false)
    private OffreRecrutement offreEmploi;

    @Column(name = "date_candidature", nullable = false)
    private LocalDate dateCandidature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_candidature_id")
    private com.mrtovo.backend.model.StatutCandidature statutCandidature;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CandidatRecrutement getCandidat() {
        return candidat;
    }

    public void setCandidat(CandidatRecrutement candidat) {
        this.candidat = candidat;
    }

    public OffreRecrutement getOffreEmploi() {
        return offreEmploi;
    }

    public void setOffreEmploi(OffreRecrutement offreEmploi) {
        this.offreEmploi = offreEmploi;
    }

    public LocalDate getDateCandidature() {
        return dateCandidature;
    }

    public void setDateCandidature(LocalDate dateCandidature) {
        this.dateCandidature = dateCandidature;
    }

    @Transient
    public StatutCandidatureRecrutement getStatut() {
        if (statutCandidature == null || statutCandidature.getId() == null) {
            return StatutCandidatureRecrutement.EN_ATTENTE;
        }
        return switch (statutCandidature.getId().intValue()) {
            case 1 -> StatutCandidatureRecrutement.EN_ATTENTE;
            case 2 -> StatutCandidatureRecrutement.EN_COURS_EVALUATION;
            case 3 -> StatutCandidatureRecrutement.EN_ENTRETIEN;
            case 4 -> StatutCandidatureRecrutement.RETENU;
            case 5 -> StatutCandidatureRecrutement.REFUSE;
            default -> StatutCandidatureRecrutement.EN_ATTENTE;
        };
    }

    @Transient
    public void setStatut(StatutCandidatureRecrutement statut) {
        if (statut == null) {
            statutCandidature = null;
            return;
        }
        com.mrtovo.backend.model.StatutCandidature reference =
                new com.mrtovo.backend.model.StatutCandidature();
        reference.setId(switch (statut) {
            case EN_ATTENTE -> 1L;
            case EN_COURS_EVALUATION -> 2L;
            case EN_ENTRETIEN -> 3L;
            case RETENU, PROPOSITION_ENVOYEE, EN_NEGOCIATION, EMBAUCHE -> 4L;
            case REFUSE, ABANDONNE -> 5L;
        });
        statutCandidature = reference;
    }
}
