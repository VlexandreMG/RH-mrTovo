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
import java.time.LocalDateTime;

@Entity
@Table(name = "document_salarie")
public class DocumentSalarie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEmbauche employe;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false)
    private TypeDocument typeDocument;

    @Column(nullable = false)
    private String libelle;

    private String numero;

    @Column(name = "date_emission")
    private LocalDate dateEmission;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "fichier_cle")
    private String fichierCle;

    @Column(name = "nom_fichier_original")
    private String nomFichierOriginal;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "date_depot", nullable = false)
    private LocalDateTime dateDepot = LocalDateTime.now();

    @Column(name = "depose_par", nullable = false)
    private String deposePar;

    public EtatValidite etatValidite(LocalDate aujourdhui) {
        return EtatValidite.calculer(dateExpiration, aujourdhui);
    }

    public boolean aUnFichier() {
        return fichierCle != null;
    }

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

    public TypeDocument getTypeDocument() {
        return typeDocument;
    }

    public void setTypeDocument(TypeDocument typeDocument) {
        this.typeDocument = typeDocument;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public String getFichierCle() {
        return fichierCle;
    }

    public void setFichierCle(String fichierCle) {
        this.fichierCle = fichierCle;
    }

    public String getNomFichierOriginal() {
        return nomFichierOriginal;
    }

    public void setNomFichierOriginal(String nomFichierOriginal) {
        this.nomFichierOriginal = nomFichierOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(LocalDateTime dateDepot) {
        this.dateDepot = dateDepot;
    }

    public String getDeposePar() {
        return deposePar;
    }

    public void setDeposePar(String deposePar) {
        this.deposePar = deposePar;
    }
}
