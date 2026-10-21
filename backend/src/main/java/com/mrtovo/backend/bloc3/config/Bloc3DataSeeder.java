package com.mrtovo.backend.bloc3.config;

import com.mrtovo.backend.bloc3.domain.Candidat;
import com.mrtovo.backend.bloc3.domain.Candidature;
import com.mrtovo.backend.bloc3.domain.Departement;
import com.mrtovo.backend.bloc3.domain.GrilleSalariale;
import com.mrtovo.backend.bloc3.domain.OffreEmploi;
import com.mrtovo.backend.bloc3.domain.Poste;
import com.mrtovo.backend.bloc3.domain.StatutCandidature;
import com.mrtovo.backend.bloc3.domain.StatutOffreEmploi;
import com.mrtovo.backend.bloc3.domain.StatutPoste;
import com.mrtovo.backend.bloc3.repository.CandidatRepository;
import com.mrtovo.backend.bloc3.repository.CandidatureRepository;
import com.mrtovo.backend.bloc3.repository.DepartementRepository;
import com.mrtovo.backend.bloc3.repository.GrilleSalarialeRepository;
import com.mrtovo.backend.bloc3.repository.OffreEmploiRepository;
import com.mrtovo.backend.bloc3.repository.PosteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Donnees de demonstration minimales : le module Bloc 3 doit etre utilisable
 * meme si les blocs 1 et 2 n'ont pas encore produit de donnees.
 * Le seeding est idempotent et n'efface jamais de donnees existantes.
 */
@Component
@Order(2)
public class Bloc3DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Bloc3DataSeeder.class);
    private static final String TITRE_POSTE = "Developpeur Java / Spring Boot";
    private static final String TITRE_OFFRE = "Developpeur Java / Spring Boot";

    private final Bloc3Properties properties;
    private final DepartementRepository departementRepository;
    private final PosteRepository posteRepository;
    private final GrilleSalarialeRepository grilleSalarialeRepository;
    private final OffreEmploiRepository offreEmploiRepository;
    private final CandidatRepository candidatRepository;
    private final CandidatureRepository candidatureRepository;

    public Bloc3DataSeeder(Bloc3Properties properties,
                           DepartementRepository departementRepository,
                           PosteRepository posteRepository,
                           GrilleSalarialeRepository grilleSalarialeRepository,
                           OffreEmploiRepository offreEmploiRepository,
                           CandidatRepository candidatRepository,
                           CandidatureRepository candidatureRepository) {
        this.properties = properties;
        this.departementRepository = departementRepository;
        this.posteRepository = posteRepository;
        this.grilleSalarialeRepository = grilleSalarialeRepository;
        this.offreEmploiRepository = offreEmploiRepository;
        this.candidatRepository = candidatRepository;
        this.candidatureRepository = candidatureRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isDonneesDemo()) {
            return;
        }
        try {
            ensurePosteEtGrille();
            ensureOffreEtCandidatureRetenue();
        } catch (RuntimeException exception) {
            log.warn("Initialisation des donnees de demonstration impossible : {}", exception.getMessage());
        }
    }

    private void ensurePosteEtGrille() {
        boolean posteExistant = posteRepository.findAll().stream()
                .anyMatch(poste -> TITRE_POSTE.equalsIgnoreCase(poste.getTitre()));
        if (!posteExistant) {
            Departement departement = departementRepository.findAll().stream()
                    .filter(item -> "Informatique".equalsIgnoreCase(item.getNom()))
                    .findFirst()
                    .orElseGet(() -> {
                        Departement nouveau = new Departement();
                        nouveau.setNom("Informatique");
                        nouveau.setCode("INFO");
                        return departementRepository.save(nouveau);
                    });
            Poste poste = new Poste();
            poste.setTitre(TITRE_POSTE);
            poste.setDescription("Developpement backend Java / Spring Boot");
            poste.setDepartement(departement);
            poste.setStatut(StatutPoste.ACTIF);
            poste = posteRepository.save(poste);

            creerNiveau(poste, "CONFIRME", "3000000", "4500000");
            creerNiveau(poste, "SENIOR", "4500000", "6000000");
            log.info("Poste de demonstration '{}' cree", TITRE_POSTE);
        }
    }

    private void creerNiveau(Poste poste, String niveau, String min, String max) {
        GrilleSalariale grille = new GrilleSalariale();
        grille.setPoste(poste);
        grille.setNiveau(niveau);
        grille.setSalaireMin(new BigDecimal(min));
        grille.setSalaireMax(new BigDecimal(max));
        grille.setDateDebut(LocalDate.now().withDayOfYear(1));
        grilleSalarialeRepository.save(grille);
    }

    private void ensureOffreEtCandidatureRetenue() {
        boolean candidatureRetenue = candidatureRepository.findAll().stream()
                .anyMatch(candidature -> candidature.getStatut() == StatutCandidature.RETENU);
        if (candidatureRetenue) {
            return;
        }
        OffreEmploi offre = offreEmploiRepository.findAll().stream()
                .filter(item -> item.getStatut() == StatutOffreEmploi.PUBLIEE)
                .findFirst()
                .orElseGet(() -> {
                    OffreEmploi nouvelle = new OffreEmploi();
                    nouvelle.setTitre(TITRE_OFFRE);
                    nouvelle.setDescription("Developpeur backend maitrisant Java et Spring Boot");
                    nouvelle.setDatePublication(LocalDate.now());
                    nouvelle.setStatut(StatutOffreEmploi.PUBLIEE);
                    return offreEmploiRepository.save(nouvelle);
                });

        Candidat candidat = new Candidat();
        candidat.setNom("Doe");
        candidat.setPrenom("John");
        candidat.setEmail("john.doe@example.mg");
        candidat.setTelephone("034 00 000 00");
        candidat = candidatRepository.save(candidat);

        Candidature candidature = new Candidature();
        candidature.setCandidat(candidat);
        candidature.setOffreEmploi(offre);
        candidature.setDateCandidature(LocalDate.now().minusDays(5));
        candidature.setDateStatut(LocalDate.now());
        candidature.setStatut(StatutCandidature.RETENU);
        candidature.setCommentaire("Candidat retenu a l'issue des entretiens");
        candidatureRepository.save(candidature);
        log.info("Candidature de demonstration au statut RETENU creee");
    }

    public List<Poste> postes() {
        return posteRepository.findAll();
    }
}
