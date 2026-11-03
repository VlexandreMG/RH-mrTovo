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

@Entity(name = "RecrutementEmploye")
@Table(name = "employe")
public class EmployeEmbauche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String matricule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false, unique = true)
    private CandidatRecrutement candidat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposition_id", nullable = false, unique = true)
    private PropositionOffre proposition;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    private String telephone;

    private String adresse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private PosteRecrutement poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private DepartementRecrutement departement;

    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    @Column(name = "date_entree", nullable = false)
    private LocalDate dateEntree;

    @Column(name = "date_fin_contrat")
    private LocalDate dateFinContrat;

    @Column(name = "date_fin_periode_essai")
    private LocalDate dateFinPeriodeEssai;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEmploye statut = StatutEmploye.EN_PERIODE_DESSAI;

    // ----- Bloc 5 : identite complete du dossier administratif -----

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance")
    private String lieuNaissance;

    @Column(length = 1)
    private String sexe;

    private String nationalite;

    @Column(name = "numero_cin")
    private String numeroCin;

    @Column(name = "numero_cnaps")
    private String numeroCnaps;

    @Column(name = "situation_familiale")
    private String situationFamiliale;

    @Column(name = "nombre_enfants")
    private Integer nombreEnfants;

    @Column(name = "contact_urgence_nom")
    private String contactUrgenceNom;

    @Column(name = "contact_urgence_telephone")
    private String contactUrgenceTelephone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public CandidatRecrutement getCandidat() {
        return candidat;
    }

    public void setCandidat(CandidatRecrutement candidat) {
        this.candidat = candidat;
    }

    public PropositionOffre getProposition() {
        return proposition;
    }

    public void setProposition(PropositionOffre proposition) {
        this.proposition = proposition;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
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

    public LocalDate getDateEmbauche() {
        return dateEmbauche;
    }

    public void setDateEmbauche(LocalDate dateEmbauche) {
        this.dateEmbauche = dateEmbauche;
    }

    public LocalDate getDateEntree() {
        return dateEntree;
    }

    public void setDateEntree(LocalDate dateEntree) {
        this.dateEntree = dateEntree;
    }

    public LocalDate getDateFinContrat() {
        return dateFinContrat;
    }

    public void setDateFinContrat(LocalDate dateFinContrat) {
        this.dateFinContrat = dateFinContrat;
    }

    public LocalDate getDateFinPeriodeEssai() {
        return dateFinPeriodeEssai;
    }

    public void setDateFinPeriodeEssai(LocalDate dateFinPeriodeEssai) {
        this.dateFinPeriodeEssai = dateFinPeriodeEssai;
    }

    public StatutEmploye getStatut() {
        return statut;
    }

    public void setStatut(StatutEmploye statut) {
        this.statut = statut;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getLieuNaissance() {
        return lieuNaissance;
    }

    public void setLieuNaissance(String lieuNaissance) {
        this.lieuNaissance = lieuNaissance;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public String getNationalite() {
        return nationalite;
    }

    public void setNationalite(String nationalite) {
        this.nationalite = nationalite;
    }

    public String getNumeroCin() {
        return numeroCin;
    }

    public void setNumeroCin(String numeroCin) {
        this.numeroCin = numeroCin;
    }

    public String getNumeroCnaps() {
        return numeroCnaps;
    }

    public void setNumeroCnaps(String numeroCnaps) {
        this.numeroCnaps = numeroCnaps;
    }

    public String getSituationFamiliale() {
        return situationFamiliale;
    }

    public void setSituationFamiliale(String situationFamiliale) {
        this.situationFamiliale = situationFamiliale;
    }

    public Integer getNombreEnfants() {
        return nombreEnfants;
    }

    public void setNombreEnfants(Integer nombreEnfants) {
        this.nombreEnfants = nombreEnfants;
    }

    public String getContactUrgenceNom() {
        return contactUrgenceNom;
    }

    public void setContactUrgenceNom(String contactUrgenceNom) {
        this.contactUrgenceNom = contactUrgenceNom;
    }

    public String getContactUrgenceTelephone() {
        return contactUrgenceTelephone;
    }

    public void setContactUrgenceTelephone(String contactUrgenceTelephone) {
        this.contactUrgenceTelephone = contactUrgenceTelephone;
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
