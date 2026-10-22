package com.mrtovo.backend.entity.contrat;
import java.sql.Date;

import com.mrtovo.backend.StatutContrat.StatutContrat;
import com.mrtovo.backend.entity.Employee.Employee;
import com.mrtovo.backend.entity.TypeContrat.TypeContrat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
@Entity
@Table(name = "achat")
public class Contrat{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false)
    private Long id;
    @Column(name = "dateDebut",nullable = false)
    private Date dateDebut;
    @Column(name = "dateFin", nullable = false)
    private Date dateFin;
    @OneToOne
    @JoinColumn(name = "idTypeContrat", referencedColumnName = "id")
    private TypeContrat typeContrat;
    @Column(name = "salaire", nullable = false)
    private double salaire;
    @OneToOne
    @JoinColumn(name = "idStatutContrat", referencedColumnName = "id")
    private StatutContrat statutContrat;
    @OneToOne
    @JoinColumn(name = "idEmploye", referencedColumnName = "id")
    private Employee employe;

    public Contrat() {
    }
    public Contrat(Long id, Date dateDebut, Date dateFin, TypeContrat typeContrat, double salaire, StatutContrat statutContrat, Employee employe) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.typeContrat = typeContrat;
        this.salaire = salaire;
        this.statutContrat = statutContrat;
        this.employe = employe;
    }
    public Contrat(Date dateDebut, Date dateFin, TypeContrat typeContrat, double salaire, StatutContrat statutContrat, Employee employe) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.typeContrat = typeContrat;
        this.salaire = salaire;
        this.statutContrat = statutContrat;
        this.employe = employe;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public TypeContrat getTypeContrat() {
        return typeContrat;
    }

    public void setTypeContrat(TypeContrat typeContrat) {
        this.typeContrat = typeContrat;
    }

    public double getSalaire() {
        return salaire;
    }

    public void setSalaire(double salaire) {
        this.salaire = salaire;
    }

    public StatutContrat getStatutContrat() {
        return statutContrat;
    }

    public void setStatutContrat(StatutContrat statutContrat) {
        this.statutContrat = statutContrat;
    }

    public Employee getEmploye() {
        return employe;
    }

    public void setEmploye(Employee employe) {
        this.employe = employe;
    }
}