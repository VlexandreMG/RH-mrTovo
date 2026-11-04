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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;

@Entity
@Table(name = "horaire_regime")
@Getter
@Setter
@NoArgsConstructor
public class HoraireRegime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regime_id", nullable = false)
    private RegimeTravail regime;

    @Column(name = "jour_semaine", nullable = false)
    private int jourSemaine;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(name = "pause_minutes", nullable = false)
    private int pauseMinutes;

    public int minutesPrevues() {
        return (int) Duration.between(heureDebut, heureFin).toMinutes() - pauseMinutes;
    }
}
