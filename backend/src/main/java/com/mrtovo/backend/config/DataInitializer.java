package com.mrtovo.backend.config;

import com.mrtovo.backend.entity.*;
import com.mrtovo.backend.repository.*;
import com.mrtovo.backend.service.DemandeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Données de démonstration, insérées uniquement si les tables sont vides. */
@Configuration
public class DataInitializer {

    /** Marqueur des mouvements créés par le seeder de soldes : rend l'initialisation rejouable. */
    private static final String AUTEUR_DEMO_SOLDE = "demo-solde";

    @Bean
    CommandLineRunner initData(DepartementRepository departementRepository,
                               PosteRepository posteRepository,
                               NiveauPosteRepository niveauRepository,
                               UtilisateurRepository utilisateurRepository,
                               GrilleSalarialeRepository grilleRepository,
                               EmployeRepository employeRepository,
                               PasswordEncoder encoder) {
        return args -> {
            if (departementRepository.count() == 0) {
                for (String nom : List.of("Informatique", "Ressources Humaines", "Finance", "Direction", "Commercial")) {
                    Departement d = new Departement();
                    d.setNom(nom);
                    departementRepository.save(d);
                }
            }

            if (niveauRepository.count() == 0) {
                String[] libelles = {"Junior", "Confirmé", "Senior"};
                for (int i = 0; i < libelles.length; i++) {
                    NiveauPoste n = new NiveauPoste();
                    n.setLibelle(libelles[i]);
                    n.setRang(i + 1);
                    niveauRepository.save(n);
                }
            }

            if (posteRepository.count() == 0) {
                Map<String, String> postes = Map.of(
                        "Développeur", "Conception, développement et maintenance des applications",
                        "Comptable", "Tenue de la comptabilité et suivi financier",
                        "Chargé RH", "Gestion administrative et accompagnement du personnel",
                        "Commercial", "Développement du portefeuille clients");
                postes.forEach((intitule, description) -> {
                    Poste p = new Poste();
                    p.setIntitule(intitule);
                    p.setDescription(description);
                    posteRepository.save(p);
                });
            }

            // Grille : base mensuelle par poste, majorée selon le niveau (Junior x1, Confirmé x1.5, Senior x2).
            if (grilleRepository.count() == 0) {
                Map<String, long[]> bases = Map.of(
                        "Développeur", new long[]{1_200_000, 1_800_000},
                        "Comptable", new long[]{900_000, 1_400_000},
                        "Chargé RH", new long[]{800_000, 1_200_000},
                        "Commercial", new long[]{700_000, 1_100_000});
                double[] coef = {1.0, 1.5, 2.0};
                List<NiveauPoste> niveaux = niveauRepository.findAllByOrderByRangAsc();
                for (Poste poste : posteRepository.findAll()) {
                    long[] base = bases.get(poste.getIntitule());
                    for (int i = 0; i < niveaux.size(); i++) {
                        GrilleSalariale g = new GrilleSalariale();
                        g.setPoste(poste);
                        g.setNiveau(niveaux.get(i));
                        g.setSalaireMin(BigDecimal.valueOf(Math.round(base[0] * coef[i])));
                        g.setSalaireMax(BigDecimal.valueOf(Math.round(base[1] * coef[i])));
                        g.setDateDebut(LocalDate.now().withDayOfYear(1));
                        grilleRepository.save(g);
                    }
                }
            }

            // Chaque employé est ajouté indépendamment : une base déjà peuplée reçoit les nouveaux matricules.
            Departement info = departementRepository.findAll().stream()
                    .filter(d -> d.getNom().equals("Informatique")).findFirst().orElse(null);
            Poste dev = posteRepository.findAll().stream()
                    .filter(p -> p.getIntitule().equals("Développeur")).findFirst().orElse(null);
            List<NiveauPoste> niveaux = niveauRepository.findAllByOrderByRangAsc();
            if (info != null && dev != null && !niveaux.isEmpty()) {
                String[][] data = {
                        {"Rakoto Jean", "EMP-001", "0"},
                        {"Rasoa Marie", "EMP-002", "2"},
                        {"Andry Faly", "EMP-003", "1"}};
                for (String[] row : data) {
                    if (employeRepository.existsByMatricule(row[1])) {
                        continue;
                    }
                    Employe e = new Employe();
                    e.setNom(row[0]);
                    e.setMatricule(row[1]);
                    e.setPoste(dev);
                    e.setDepartement(info);
                    int indexNiveau = Math.min(Integer.parseInt(row[2]), niveaux.size() - 1);
                    e.setNiveau(niveaux.get(indexNiveau));
                    e.setDateEmbauche(LocalDate.of(2022, 3, 10));
                    e.setActif(true);
                    employeRepository.save(e);
                }
            }

            // Chaque compte est ajouté indépendamment : la présence d'autres utilisateurs ne bloque plus.
            Map<String, Departement> deps = new java.util.HashMap<>();
            departementRepository.findAll().forEach(d -> deps.put(d.getNom(), d));

            creerUtilisateur(utilisateurRepository, encoder, "Manager Informatique", "manager.informatique@rh.com", "manager123", RoleUtilisateur.MANAGER, deps.get("Informatique"));
            creerUtilisateur(utilisateurRepository, encoder, "Manager Ressources Humaines", "manager.rh@rh.com", "manager123", RoleUtilisateur.MANAGER, deps.get("Ressources Humaines"));
            creerUtilisateur(utilisateurRepository, encoder, "Manager Finance", "manager.finance@rh.com", "manager123", RoleUtilisateur.MANAGER, deps.get("Finance"));
            creerUtilisateur(utilisateurRepository, encoder, "Manager Direction", "manager.direction@rh.com", "manager123", RoleUtilisateur.MANAGER, deps.get("Direction"));
            creerUtilisateur(utilisateurRepository, encoder, "Manager Commercial", "manager.commercial@rh.com", "manager123", RoleUtilisateur.MANAGER, deps.get("Commercial"));
            creerUtilisateur(utilisateurRepository, encoder, "Responsable RH", "rh@rh.com", "rh123", RoleUtilisateur.RH, null);
            creerUtilisateur(utilisateurRepository, encoder, "Responsable Finance", "finance@rh.com", "finance123", RoleUtilisateur.FINANCE, null);
            creerUtilisateur(utilisateurRepository, encoder, "Directeur Général", "direction@rh.com", "direction123", RoleUtilisateur.DIRECTION, null);
            creerUtilisateur(utilisateurRepository, encoder, "Employee Finance", "emp@mail.com", "emp123", RoleUtilisateur.EMPLOYE, deps.get("Finance"));
        };
    }

    /**
     * Bloc 7 : deux dossiers de démonstration pour visualiser des soldes variés sans script SQL.
     * Aina a ~20 mois d'ancienneté (solde confortable), Luc ~5 mois (solde plus serré).
     */
    @Bean
    CommandLineRunner initSoldesCongeDemo(EmployeEmbaucheRepository employeRepository,
                                          ContratRepository contratRepository,
                                          TypeCongeRepository typeCongeRepository,
                                          DroitCongeRepository droitCongeRepository,
                                          DemandeCongeRepository demandeCongeRepository,
                                          UtilisateurRepository utilisateurRepository,
                                          PasswordEncoder encoder) {
        return args -> {
            TypeConge congePaye = typeCongeRepository.findByCodeIgnoreCase("CONGE_PAYE").orElse(null);
            if (congePaye == null) {
                return;
            }

            LocalDate lundi = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            String moisPaie = YearMonth.now().minusMonths(1).toString();

            EmployeEmbauche aina = employeDemo(employeRepository, contratRepository,
                    "EMP-DEMO-SOLDE-01", "Aina", "Ranaivo", "demo.solde.01@rh.com",
                    LocalDate.now().minusMonths(20), new BigDecimal("1250000"));
            EmployeEmbauche luc = employeDemo(employeRepository, contratRepository,
                    "EMP-DEMO-SOLDE-02", "Luc", "Andria", "demo.solde.02@rh.com",
                    LocalDate.now().minusMonths(5).minusDays(10), new BigDecimal("900000"));

            // Comptes de connexion : les soldes sont ainsi visibles dans l'espace salarie.
            creerCompteDemo(utilisateurRepository, encoder, aina);
            creerCompteDemo(utilisateurRepository, encoder, luc);

            if (!dejaInitialise(droitCongeRepository, demandeCongeRepository, aina)) {
                ajouterDroit(droitCongeRepository, aina, TypeDroitConge.AJUSTEMENT,
                        "3", "Prime d'anciennete (demo)", null, null);
                ajouterDemande(demandeCongeRepository, aina, congePaye, lundi.minusWeeks(10), 5,
                        "Conges d'ete (demo)", StatutDemandeConge.VALIDEE);
                ajouterDemande(demandeCongeRepository, aina, congePaye, lundi.minusWeeks(5), 2,
                        "Rendez-vous personnel (demo)", StatutDemandeConge.VALIDEE);
                ajouterDemande(demandeCongeRepository, aina, congePaye, lundi.plusWeeks(4), 3,
                        "Long week-end (demo)", StatutDemandeConge.EN_ATTENTE_RH);
                ajouterDroit(droitCongeRepository, aina, TypeDroitConge.INDEMNISATION,
                        "-2", "Jours non pris (demo)", moisPaie, "83333.33");
            }

            if (!dejaInitialise(droitCongeRepository, demandeCongeRepository, luc)) {
                ajouterDroit(droitCongeRepository, luc, TypeDroitConge.AJUSTEMENT,
                        "2", "Conge supplementaire negocie (demo)", null, null);
                ajouterDemande(demandeCongeRepository, luc, congePaye, lundi.minusWeeks(3), 2,
                        "Demarche administrative (demo)", StatutDemandeConge.VALIDEE);
                ajouterDemande(demandeCongeRepository, luc, congePaye, lundi.plusWeeks(2), 5,
                        "Conges de fin d'annee (demo)", StatutDemandeConge.EN_ATTENTE_MANAGER);
                ajouterDemande(demandeCongeRepository, luc, congePaye, lundi.plusWeeks(6), 5,
                        "Conges refusees (demo)", StatutDemandeConge.REFUSEE);
                ajouterDroit(droitCongeRepository, luc, TypeDroitConge.INDEMNISATION,
                        "-1", "Jour non pris (demo)", moisPaie, "30000.00");
            }
        };
    }

    private EmployeEmbauche employeDemo(EmployeEmbaucheRepository employeRepository,
                                        ContratRepository contratRepository,
                                        String matricule, String prenom, String nom, String email,
                                        LocalDate dateEntree, BigDecimal salaire) {
        EmployeEmbauche employe = employeRepository.findByMatricule(matricule).orElseGet(() -> {
            EmployeEmbauche nouveau = new EmployeEmbauche();
            nouveau.setMatricule(matricule);
            nouveau.setPrenom(prenom);
            nouveau.setNom(nom);
            nouveau.setEmail(email);
            nouveau.setDateEmbauche(dateEntree);
            nouveau.setDateEntree(dateEntree);
            nouveau.setStatut(StatutEmploye.ACTIF);
            nouveau.setDossierMinimal(false);
            return employeRepository.save(nouveau);
        });

        if (contratRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId()).isEmpty()) {
            Contrat contrat = new Contrat();
            contrat.setEmploye(employe);
            contrat.setReference("CTR-" + matricule);
            contrat.setTypeContrat(TypeContrat.CDI);
            contrat.setDateDebut(dateEntree);
            contrat.setSalaire(salaire);
            contrat.setStatutContrat(StatutContrat.ACTIF);
            contratRepository.save(contrat);
        }
        return employe;
    }

    private void creerCompteDemo(UtilisateurRepository utilisateurRepository, PasswordEncoder encodeur,
                                 EmployeEmbauche employe) {
        if (utilisateurRepository.findByEmployeId(employe.getId()).isPresent()
                || utilisateurRepository.existsByEmailIgnoreCase(employe.getEmail())) {
            return;
        }
        Utilisateur compte = new Utilisateur();
        compte.setNom(employe.getNomComplet());
        compte.setEmail(employe.getEmail().toLowerCase(Locale.ROOT));
        compte.setMotDePasse(encodeur.encode("salarie123"));
        compte.setRole(RoleUtilisateur.EMPLOYE);
        compte.setEmploye(employe);
        utilisateurRepository.save(compte);
    }

    private boolean dejaInitialise(DroitCongeRepository droitCongeRepository,
                                   DemandeCongeRepository demandeCongeRepository,
                                   EmployeEmbauche employe) {
        boolean droits = droitCongeRepository.findByEmployeIdOrderByDateCreationDesc(employe.getId()).stream()
                .anyMatch(droit -> AUTEUR_DEMO_SOLDE.equals(droit.getCreePar()));
        boolean demandes = demandeCongeRepository.findByEmployeIdOrderByDateDebutDesc(employe.getId()).stream()
                .anyMatch(demande -> AUTEUR_DEMO_SOLDE.equals(demande.getDemandePar()));
        return droits || demandes;
    }

    private void ajouterDroit(DroitCongeRepository droitCongeRepository, EmployeEmbauche employe,
                              TypeDroitConge type, String jours, String motif,
                              String moisPaie, String montant) {
        DroitConge droit = new DroitConge();
        droit.setEmploye(employe);
        droit.setTypeDroit(type);
        droit.setJours(new BigDecimal(jours));
        droit.setMotif(motif);
        droit.setMoisPaie(moisPaie);
        droit.setMontant(montant != null ? new BigDecimal(montant) : null);
        droit.setCreePar(AUTEUR_DEMO_SOLDE);
        droitCongeRepository.save(droit);
    }

    private void ajouterDemande(DemandeCongeRepository demandeCongeRepository, EmployeEmbauche employe,
                                TypeConge type, LocalDate debut, int nbJours, String motif,
                                StatutDemandeConge statut) {
        DemandeConge demande = new DemandeConge();
        demande.setEmploye(employe);
        demande.setTypeConge(type);
        demande.setDateDebut(debut);
        demande.setDateFin(debut.plusDays(nbJours - 1L));
        demande.setNbJours(nbJours);
        demande.setMotif(motif);
        demande.setStatut(statut);
        demande.setDemandePar(AUTEUR_DEMO_SOLDE);
        if (statut == StatutDemandeConge.VALIDEE || statut == StatutDemandeConge.REFUSEE) {
            demande.setDecidePar("rh@rh.com");
            demande.setDateDecision(java.time.LocalDateTime.now());
            demande.setCommentaireRh(statut == StatutDemandeConge.VALIDEE ? "Validee (demo)" : "Refusee (demo)");
        }
        demandeCongeRepository.save(demande);
    }

    /** Chaque demande doit avoir son offre d'emploi : rattrape celles créées avant ce lien. */
    @Bean
    CommandLineRunner lierDemandesAuxOffres(DemandeService demandeService) {
        return args -> demandeService.creerOffresManquantes();
    }

   private void creerUtilisateur(UtilisateurRepository repo, PasswordEncoder encoder,
                              String nom, String email, String mdp, RoleUtilisateur role,
                              Departement departement) {
    if (repo.existsByEmailIgnoreCase(email)) {
        return;
    }
    Utilisateur u = new Utilisateur();
    u.setNom(nom);
    u.setEmail(email);
    u.setMotDePasse(encoder.encode(mdp));
    u.setRole(role);
    u.setDepartement(departement);
    repo.save(u);
}
}
