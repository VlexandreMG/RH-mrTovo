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

import java.time.LocalDateTime;

/**
 * Trace immuable d'une action sur un dossier salarie.
 * Toutes les colonnes sont {@code updatable = false} : une fois ecrite, la ligne ne change plus.
 */
@Entity
@Table(name = "journal_audit")
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false, updatable = false)
    private EmployeEmbauche employe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ActionAudit action;

    @Column(nullable = false, updatable = false)
    private String auteur;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "date_action", nullable = false, updatable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    protected JournalAudit() {
        // requis par JPA
    }

    public JournalAudit(EmployeEmbauche employe, ActionAudit action, String auteur, String details) {
        this.employe = employe;
        this.action = action;
        this.auteur = auteur;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public EmployeEmbauche getEmploye() {
        return employe;
    }

    public ActionAudit getAction() {
        return action;
    }

    public String getAuteur() {
        return auteur;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getDateAction() {
        return dateAction;
    }
}
