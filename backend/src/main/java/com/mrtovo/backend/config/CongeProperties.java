package com.mrtovo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Reglages du Bloc 7, modifiables dans application.properties sans toucher au code. */
@ConfigurationProperties(prefix = "bloc7")
public class CongeProperties {

    /** Jours de conge gagnes par mois de service complet. */
    private BigDecimal joursAcquisParMois = new BigDecimal("2.5");

    public BigDecimal getJoursAcquisParMois() {
        return joursAcquisParMois;
    }

    public void setJoursAcquisParMois(BigDecimal joursAcquisParMois) {
        this.joursAcquisParMois = joursAcquisParMois;
    }
}
