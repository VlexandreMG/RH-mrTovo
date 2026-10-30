package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.GrilleSalariale;
import com.mrtovo.backend.entity.NiveauPoste;
import com.mrtovo.backend.entity.Poste;
import com.mrtovo.backend.exception.BusinessException;
import com.mrtovo.backend.repository.GrilleSalarialeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class GrilleService {

    private final GrilleSalarialeRepository grilleRepository;

    public GrilleService(GrilleSalarialeRepository grilleRepository) {
        this.grilleRepository = grilleRepository;
    }

    /** Grille en vigueur à une date donnée pour un poste et un niveau. */
    @Transactional(readOnly = true)
    public Optional<GrilleSalariale> applicable(Poste poste, NiveauPoste niveau, LocalDate date) {
        return grilleRepository.findByPosteAndNiveauOrderByDateDebutDesc(poste, niveau).stream()
                .filter(g -> !g.getDateDebut().isAfter(date))
                .filter(g -> g.getDateFin() == null || !g.getDateFin().isBefore(date))
                .findFirst();
    }

    /**
     * Création de poste : définit une nouvelle fourchette salariale valable à partir d'aujourd'hui.
     * L'ancienne grille ouverte est clôturée pour garder l'historique.
     */
    @Transactional
    public GrilleSalariale definir(Poste poste, NiveauPoste niveau, BigDecimal min, BigDecimal max) {
        if (min.signum() < 0 || max.compareTo(min) < 0) {
            throw new BusinessException("Fourchette salariale invalide : le maximum doit être supérieur ou égal au minimum.");
        }
        LocalDate aujourdhui = LocalDate.now();
        var existantes = grilleRepository.findByPosteAndNiveauOrderByDateDebutDesc(poste, niveau);

        for (GrilleSalariale g : existantes) {
            if (g.getDateDebut().equals(aujourdhui)) {
                g.setSalaireMin(min);
                g.setSalaireMax(max);
                g.setDateFin(null);
                return grilleRepository.save(g);
            }
        }
        for (GrilleSalariale g : existantes) {
            if (g.getDateDebut().isBefore(aujourdhui) && (g.getDateFin() == null || !g.getDateFin().isBefore(aujourdhui))) {
                g.setDateFin(aujourdhui.minusDays(1));
                grilleRepository.save(g);
            }
        }
        GrilleSalariale nouvelle = new GrilleSalariale();
        nouvelle.setPoste(poste);
        nouvelle.setNiveau(niveau);
        nouvelle.setSalaireMin(min);
        nouvelle.setSalaireMax(max);
        nouvelle.setDateDebut(aujourdhui);
        return grilleRepository.save(nouvelle);
    }
}
