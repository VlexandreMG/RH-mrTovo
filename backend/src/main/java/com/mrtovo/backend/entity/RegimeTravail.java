package com.mrtovo.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Modele d'horaires reutilisable ; jamais modifie apres creation pour ne pas changer le passe. */
@Entity
@Table(name = "regime_travail")
@Getter
@Setter
@NoArgsConstructor
public class RegimeTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String libelle;

    @Column(name = "tolerance_retard_minutes", nullable = false)
    private int toleranceRetardMinutes = 10;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "cree_par", nullable = false, length = 150)
    private String creePar;

    @OneToMany(mappedBy = "regime", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("jourSemaine ASC")
    private List<HoraireRegime> horaires = new ArrayList<>();

    public void ajouterHoraire(HoraireRegime horaire) {
        horaire.setRegime(this);
        horaires.add(horaire);
    }

    /** Horaire du jour (1 = lundi ... 7 = dimanche), ou null si c'est un jour de repos. */
    public HoraireRegime horaireDu(int jourSemaine) {
        return horaires.stream()
                .filter(horaire -> horaire.getJourSemaine() == jourSemaine)
                .findFirst()
                .orElse(null);
    }

    public int minutesHebdomadaires() {
        return horaires.stream().mapToInt(HoraireRegime::minutesPrevues).sum();
    }
}
