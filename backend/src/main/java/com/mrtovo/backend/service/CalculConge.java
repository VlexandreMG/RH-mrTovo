package com.mrtovo.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/** Regles de calcul des conges (Bloc 7). Classe pure : ni base de donnees ni Spring. */
public final class CalculConge {

    /** Formule du cours : conge = salaire brut x jours non pris / 30. */
    static final BigDecimal JOURS_PAR_MOIS_DE_PAIE = BigDecimal.valueOf(30);

    private CalculConge() {
    }

    /** Jours calendaires, bornes incluses : du lundi au vendredi = 5, du lundi au lundi suivant = 8. */
    public static int joursCalendaires(LocalDate debut, LocalDate fin) {
        return (int) ChronoUnit.DAYS.between(debut, fin) + 1;
    }

    /** Seuls les mois entierement effectues ouvrent un droit. */
    public static int moisComplets(LocalDate dateEntree, LocalDate date) {
        if (date.isBefore(dateEntree)) {
            return 0;
        }
        return (int) Period.between(dateEntree, date).toTotalMonths();
    }

    public static BigDecimal joursAcquis(int moisComplets, BigDecimal joursParMois) {
        return joursParMois.multiply(BigDecimal.valueOf(moisComplets));
    }

    /** Salaire brut x jours / 30 : sert a l'indemnite de conge comme a la retenue d'une journee. */
    public static BigDecimal montantJours(BigDecimal salaireBrut, BigDecimal jours) {
        return salaireBrut.multiply(jours).divide(JOURS_PAR_MOIS_DE_PAIE, 2, RoundingMode.HALF_UP);
    }
}
