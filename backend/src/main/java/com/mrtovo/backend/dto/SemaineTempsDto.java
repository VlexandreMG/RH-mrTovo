package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.StatutHeuresSup;

import java.time.LocalDate;
import java.util.List;

public record SemaineTempsDto(
        Long employeId,
        String matricule,
        String nomComplet,
        String regimeLibelle,
        LocalDate semaineDebut,
        LocalDate semaineFin,
        List<JourTempsDto> jours,
        int minutesPrevues,
        int minutesTravaillees,
        int minutesRetard,
        int minutesDepartAnticipe,
        int joursAbsents,
        int nbAnomalies,
        Decision decision,
        Bilan bilan) {

    /** Etat des heures sup de la semaine et ce qui empeche, le cas echeant, de decider. */
    public record Decision(
            StatutHeuresSup statut,
            int minutesBrutes,
            int minutesValidees,
            boolean validable,
            String raisonBlocage,
            String commentaire,
            String decidePar) {
    }

    /** Resultat de la compensation entre minutes dues et heures sup validees. */
    public record Bilan(
            int minutesDues,
            int minutesCompensees,
            int minutesRetenues,
            int hsPayees,
            int hsPalier1,
            int hsPalier2) {
    }
}
