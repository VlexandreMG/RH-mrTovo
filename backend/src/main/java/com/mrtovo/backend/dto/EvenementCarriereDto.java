package com.mrtovo.backend.dto;

import java.time.LocalDate;

/** Une ligne de la frise chronologique de carriere (embauche, contrat, salaire, affectation...). */
public record EvenementCarriereDto(
        LocalDate date,
        String type,
        String titre,
        String description) {
}
