package com.mrtovo.backend.config;

import com.mrtovo.backend.entity.CandidatRecrutement;
import com.mrtovo.backend.entity.CandidatureSelection;
import com.mrtovo.backend.entity.DepartementRecrutement;
import com.mrtovo.backend.entity.GrilleSalarialeRecrutement;
import com.mrtovo.backend.entity.OffreRecrutement;
import com.mrtovo.backend.entity.PosteRecrutement;
import com.mrtovo.backend.entity.StatutCandidatureRecrutement;
import com.mrtovo.backend.entity.StatutOffreRecrutement;
import com.mrtovo.backend.entity.StatutPoste;
import com.mrtovo.backend.repository.CandidatRecrutementRepository;
import com.mrtovo.backend.repository.CandidatureSelectionRepository;
import com.mrtovo.backend.repository.DepartementRecrutementRepository;
import com.mrtovo.backend.repository.GrilleRecrutementRepository;
import com.mrtovo.backend.repository.OffreRecrutementRepository;
import com.mrtovo.backend.repository.PosteRecrutementRepository;
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
public class RecrutementDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RecrutementDataSeeder.class);
    private static final String TITRE_POSTE = "Developpeur Java / Spring Boot";
    private static final String TITRE_OFFRE = "Developpeur Java / Spring Boot";

    private final RecrutementProperties properties;
    private final DepartementRecrutementRepository departementRepository;
    private final PosteRecrutementRepository posteRepository;
    private final GrilleRecrutementRepository grilleSalarialeRepository;
    private final OffreRecrutementRepository offreEmploiRepository;
    private final CandidatRecrutementRepository candidatRepository;
    private final CandidatureSelectionRepository candidatureRepository;

    public RecrutementDataSeeder(RecrutementProperties properties,
                           DepartementRecrutementRepository departementRepository,
                           PosteRecrutementRepository posteRepository,
                           GrilleRecrutementRepository grilleSalarialeRepository,
                           OffreRecrutementRepository offreEmploiRepository,
                           CandidatRecrutementRepository candidatRepository,
                           CandidatureSelectionRepository candidatureRepository) {
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
            DepartementRecrutement departement = departementRepository.findAll().stream()
                    .filter(item -> "Informatique".equalsIgnoreCase(item.getNom()))
                    .findFirst()
                    .orElseGet(() -> {
                        DepartementRecrutement nouveau = new DepartementRecrutement();
                        nouveau.setNom("Informatique");
                        nouveau.setCode("INFO");
                        return departementRepository.save(nouveau);
                    });
            PosteRecrutement poste = new PosteRecrutement();
            poste.setTitre(TITRE_POSTE);
            poste.setDescription("Developpement backend Java / Spring Boot");
            poste.setDepartement(departement);
            poste.setStatut(StatutPoste.ACTIF);
            poste = posteRepository.save(poste);

            creerNiveau(poste, "CONFIRME", "3000000", "4500000");
            creerNiveau(poste, "SENIOR", "4500000", "6000000");
            log.info("PosteRecrutement de demonstration '{}' cree", TITRE_POSTE);
        }
    }

    private void creerNiveau(PosteRecrutement poste, String niveau, String min, String max) {
        GrilleSalarialeRecrutement grille = new GrilleSalarialeRecrutement();
        grille.setPoste(poste);
        grille.setNiveau(niveau);
        grille.setSalaireMin(new BigDecimal(min));
        grille.setSalaireMax(new BigDecimal(max));
        grille.setDateDebut(LocalDate.now().withDayOfYear(1));
        grilleSalarialeRepository.save(grille);
    }

    private void ensureOffreEtCandidatureRetenue() {
        boolean candidatureRetenue = candidatureRepository.findAll().stream()
                .anyMatch(candidature -> candidature.getStatut() == StatutCandidatureRecrutement.RETENU);
        if (candidatureRetenue) {
            return;
        }
        OffreRecrutement offre = offreEmploiRepository.findAll().stream()
                .filter(item -> item.getStatut() == StatutOffreRecrutement.PUBLIEE)
                .findFirst()
                .orElseGet(() -> {
                    OffreRecrutement nouvelle = new OffreRecrutement();
                    nouvelle.setTitre(TITRE_OFFRE);
                    nouvelle.setDescription("Developpeur backend maitrisant Java et Spring Boot");
                    nouvelle.setDatePublication(LocalDate.now());
                    nouvelle.setStatut(StatutOffreRecrutement.PUBLIEE);
                    return offreEmploiRepository.save(nouvelle);
                });

        final String emailDemo = "john.doe@example.mg";
        CandidatRecrutement candidat = candidatRepository.findByEmailIgnoreCase(emailDemo)
                .orElseGet(() -> {
                    CandidatRecrutement nouveau = new CandidatRecrutement();
                    nouveau.setNom("Doe");
                    nouveau.setPrenom("John");
                    nouveau.setEmail(emailDemo);
                    nouveau.setTelephone("034 00 000 00");
                    return candidatRepository.save(nouveau);
                });

        boolean candidatureDemoExistante = candidatureRepository.findByOffreEmploiId(offre.getId()).stream()
                .anyMatch(item -> item.getCandidat() != null
                        && emailDemo.equalsIgnoreCase(item.getCandidat().getEmail()));
        if (candidatureDemoExistante) {
            return;
        }

        CandidatureSelection candidature = new CandidatureSelection();
        candidature.setCandidat(candidat);
        candidature.setOffreEmploi(offre);
        candidature.setDateCandidature(LocalDate.now().minusDays(5));
        candidature.setStatut(StatutCandidatureRecrutement.RETENU);
        candidatureRepository.save(candidature);
        log.info("CandidatureSelection de demonstration au statut RETENU creee");
    }

    public List<PosteRecrutement> postes() {
        return posteRepository.findAll();
    }
}
