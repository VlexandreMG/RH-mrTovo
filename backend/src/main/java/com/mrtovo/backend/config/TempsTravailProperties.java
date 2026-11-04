package com.mrtovo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Reglages du Bloc 6 : modifiables dans application.properties sans toucher au code. */
@ConfigurationProperties(prefix = "bloc6")
public class TempsTravailProperties {

    /** Nombre de minutes d'heures sup payees au premier taux chaque semaine (8 h). */
    private int hsPalier1Minutes = 480;
    private int tauxHsPalier1 = 130;
    private int tauxHsPalier2 = 150;
    /** Au-dela, un retard est signale comme anomalie. */
    private int seuilRetardExcessifMinutes = 60;
    /** Au-dela, une presence journaliere est signalee comme anomalie (12 h). */
    private int seuilPresenceExcessiveMinutes = 720;

    public int getHsPalier1Minutes() {
        return hsPalier1Minutes;
    }

    public void setHsPalier1Minutes(int hsPalier1Minutes) {
        this.hsPalier1Minutes = hsPalier1Minutes;
    }

    public int getTauxHsPalier1() {
        return tauxHsPalier1;
    }

    public void setTauxHsPalier1(int tauxHsPalier1) {
        this.tauxHsPalier1 = tauxHsPalier1;
    }

    public int getTauxHsPalier2() {
        return tauxHsPalier2;
    }

    public void setTauxHsPalier2(int tauxHsPalier2) {
        this.tauxHsPalier2 = tauxHsPalier2;
    }

    public int getSeuilRetardExcessifMinutes() {
        return seuilRetardExcessifMinutes;
    }

    public void setSeuilRetardExcessifMinutes(int seuilRetardExcessifMinutes) {
        this.seuilRetardExcessifMinutes = seuilRetardExcessifMinutes;
    }

    public int getSeuilPresenceExcessiveMinutes() {
        return seuilPresenceExcessiveMinutes;
    }

    public void setSeuilPresenceExcessiveMinutes(int seuilPresenceExcessiveMinutes) {
        this.seuilPresenceExcessiveMinutes = seuilPresenceExcessiveMinutes;
    }
}
