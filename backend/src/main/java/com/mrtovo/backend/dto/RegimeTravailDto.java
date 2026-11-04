package com.mrtovo.backend.dto;

import java.time.LocalTime;
import java.util.List;

public record RegimeTravailDto(
        Long id,
        String code,
        String libelle,
        int toleranceRetardMinutes,
        int minutesHebdomadaires,
        List<Horaire> horaires) {

    public record Horaire(int jourSemaine, LocalTime heureDebut, LocalTime heureFin, int pauseMinutes) {
    }
}
