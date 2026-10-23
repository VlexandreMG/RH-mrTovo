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
import java.util.List;
import java.util.Map;

/** Données de démonstration, insérées uniquement si les tables sont vides. */
@Configuration
public class DataInitializer {

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

            if (employeRepository.count() == 0) {
                Departement info = departementRepository.findAll().stream()
                        .filter(d -> d.getNom().equals("Informatique")).findFirst().orElseThrow();
                Poste dev = posteRepository.findAll().stream()
                        .filter(p -> p.getIntitule().equals("Développeur")).findFirst().orElseThrow();
                List<NiveauPoste> niveaux = niveauRepository.findAllByOrderByRangAsc();

                String[][] data = {
                        {"Rakoto Jean", "EMP-001", "0"},
                        {"Rasoa Marie", "EMP-002", "2"},
                        {"Andry Faly", "EMP-003", "1"}};
                for (String[] row : data) {
                    Employe e = new Employe();
                    e.setNom(row[0]);
                    e.setMatricule(row[1]);
                    e.setPoste(dev);
                    e.setDepartement(info);
                    e.setNiveau(niveaux.get(Integer.parseInt(row[2])));
                    e.setDateEmbauche(LocalDate.of(2022, 3, 10));
                    e.setActif(true);
                    employeRepository.save(e);
                }
            }

          if (utilisateurRepository.count() == 0) {
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
}
        };
    }

    /** Chaque demande doit avoir son offre d'emploi : rattrape celles créées avant ce lien. */
    @Bean
    CommandLineRunner lierDemandesAuxOffres(DemandeService demandeService) {
        return args -> demandeService.creerOffresManquantes();
    }

   private void creerUtilisateur(UtilisateurRepository repo, PasswordEncoder encoder,
                              String nom, String email, String mdp, RoleUtilisateur role,
                              Departement departement) {
    Utilisateur u = new Utilisateur();
    u.setNom(nom);
    u.setEmail(email);
    u.setMotDePasse(encoder.encode(mdp));
    u.setRole(role);
    u.setDepartement(departement);
    repo.save(u);
}
}
