package com.mrtovo.backend.service;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Regles de calcul du temps de travail (Bloc 6).
 * Classe pure : ni base de donnees ni Spring, uniquement des nombres en entree et en sortie.
 * Toutes les durees sont exprimees en minutes entieres.
 */
public final class CalculTemps {

    /** En dessous de cette presence (5 h), la pause n'est pas deduite : le salarie ne l'a pas prise. */
    static final int PRESENCE_MIN_POUR_PAUSE = 300;

    private CalculTemps() {
    }

    public record HoraireJour(LocalTime debut, LocalTime fin, int pauseMinutes) {
        public int minutesPrevues() {
            return minutesEntre(debut, fin) - pauseMinutes;
        }
    }

    public record ResultatJour(int prevues, int travaillees, int retard, int departAnticipe, int heuresSup) {
    }

    public record Compensation(int minutesDues,
                               int hsValidees,
                               int compensees,
                               int hsPayees,
                               int minutesRetenues,
                               int hsPalier1,
                               int hsPalier2) {
    }

    /**
     * Calcule une journee.
     *
     * @param horaire  horaire prevu, ou null pour un jour de repos
     * @param entree   heure d'entree, ou null s'il n'y a pas de pointage
     * @param sortie   heure de sortie, ou null si elle n'est pas encore pointee
     * @param tolerance minutes de retard pardonnees ; au-dela, tout le retard compte
     */
    public static ResultatJour calculerJour(HoraireJour horaire, LocalTime entree, LocalTime sortie, int tolerance) {
        int prevues = horaire != null ? horaire.minutesPrevues() : 0;
        if (entree == null) {
            return new ResultatJour(prevues, 0, 0, 0, 0);
        }

        int retard = horaire != null ? retard(horaire, entree, tolerance) : 0;
        if (sortie == null) {
            return new ResultatJour(prevues, 0, retard, 0, 0);
        }

        int travaillees = minutesTravaillees(horaire, entree, sortie);
        if (horaire == null) {
            // Jour de repos : tout le temps travaille est du temps supplementaire.
            return new ResultatJour(0, travaillees, 0, 0, travaillees);
        }

        int depart = sortie.isBefore(horaire.fin()) ? minutesEntre(sortie, horaire.fin()) : 0;
        // Une journee tres courte ne doit pas compter deux fois les memes minutes manquantes.
        int departPlafonne = Math.min(depart, Math.max(0, prevues - travaillees - retard));

        LocalTime debutHs = entree.isAfter(horaire.fin()) ? entree : horaire.fin();
        int heuresSup = sortie.isAfter(debutHs) ? minutesEntre(debutHs, sortie) : 0;

        return new ResultatJour(prevues, travaillees, retard, departPlafonne, heuresSup);
    }

    /**
     * Compense les minutes dues (retards et departs anticipes) de la semaine par ses heures sup validees.
     * Les heures sup restantes sont ensuite reparties entre les deux paliers de majoration.
     */
    public static Compensation compenser(int minutesDues, int hsValidees, int palier1Minutes) {
        int compensees = Math.min(minutesDues, hsValidees);
        int hsPayees = hsValidees - compensees;
        int retenues = minutesDues - compensees;
        int palier1 = Math.min(hsPayees, palier1Minutes);
        int palier2 = hsPayees - palier1;
        return new Compensation(minutesDues, hsValidees, compensees, hsPayees, retenues, palier1, palier2);
    }

    private static int retard(HoraireJour horaire, LocalTime entree, int tolerance) {
        if (!entree.isAfter(horaire.debut())) {
            return 0;
        }
        LocalTime finRetard = entree.isAfter(horaire.fin()) ? horaire.fin() : entree;
        int retard = minutesEntre(horaire.debut(), finRetard);
        return retard > tolerance ? retard : 0;
    }

    private static int minutesTravaillees(HoraireJour horaire, LocalTime entree, LocalTime sortie) {
        int presence = minutesEntre(entree, sortie);
        int pause = horaire != null && presence >= PRESENCE_MIN_POUR_PAUSE ? horaire.pauseMinutes() : 0;
        return presence - pause;
    }

    private static int minutesEntre(LocalTime debut, LocalTime fin) {
        return (int) Duration.between(debut, fin).toMinutes();
    }
}
