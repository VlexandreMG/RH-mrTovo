package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.NatureCaseCalendrier;
import com.mrtovo.backend.entity.StatutDemandeConge;

import java.time.LocalDate;
import java.util.List;

/** Un mois du calendrier. detailVisible est faux pour un manager : ses cases ne portent que « absent ». */
public record CalendrierCongeDto(
        String mois,
        LocalDate du,
        LocalDate au,
        boolean detailVisible,
        List<String> departements,
        List<Ligne> salaries,
        List<Synthese> jours) {

    public record Ligne(
            Long employeId,
            String matricule,
            String nomComplet,
            String departementNom,
            List<Case> cases) {
    }

    /** Une case par jour occupe. Pour un manager, seuls date, nature et libelle sont remplis. */
    public record Case(
            LocalDate date,
            NatureCaseCalendrier nature,
            String libelle,
            Long demandeId,
            Long typeCongeId,
            StatutDemandeConge statut,
            LocalDate dateDebut,
            LocalDate dateFin) {
    }

    /** attendus : salaries prevus ce jour ; enAttente : ceux qu'une validation retirerait des presents. */
    public record Synthese(
            LocalDate date,
            int attendus,
            int presents,
            int enAttente) {
    }
}
