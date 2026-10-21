package com.mrtovo.backend.bloc3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bloc3")
public class Bloc3Properties {

    private int dureeValiditeJours = 7;
    private int periodeEssaiDefautMois = 6;
    private int dureeMaxCddMois = 24;
    private boolean donneesDemo = true;

    public int getDureeValiditeJours() {
        return dureeValiditeJours;
    }

    public void setDureeValiditeJours(int dureeValiditeJours) {
        this.dureeValiditeJours = dureeValiditeJours;
    }

    public int getPeriodeEssaiDefautMois() {
        return periodeEssaiDefautMois;
    }

    public void setPeriodeEssaiDefautMois(int periodeEssaiDefautMois) {
        this.periodeEssaiDefautMois = periodeEssaiDefautMois;
    }

    public int getDureeMaxCddMois() {
        return dureeMaxCddMois;
    }

    public void setDureeMaxCddMois(int dureeMaxCddMois) {
        this.dureeMaxCddMois = dureeMaxCddMois;
    }

    public boolean isDonneesDemo() {
        return donneesDemo;
    }

    public void setDonneesDemo(boolean donneesDemo) {
        this.donneesDemo = donneesDemo;
    }
}
