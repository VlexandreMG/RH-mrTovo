package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.SalarieProfilDto;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Relie le compte connecte a son dossier : aucune requete salarie ne transmet d'identifiant de salarie.
 * Un compte sans dossier de recrutement (RH, manager, finance, direction) recoit un dossier minimal
 * pour pouvoir demander ses conges ; ce dossier ne decompte pas de solde.
 */
@Service
@Transactional
public class EspaceSalarieService {

    private final UtilisateurRepository utilisateurRepository;
    private final EmployeEmbaucheRepository employeRepository;
    private final SoldeCongeService soldeService;
    private final CongeService congeService;

    public EspaceSalarieService(UtilisateurRepository utilisateurRepository,
                                EmployeEmbaucheRepository employeRepository,
                                SoldeCongeService soldeService,
                                CongeService congeService) {
        this.utilisateurRepository = utilisateurRepository;
        this.employeRepository = employeRepository;
        this.soldeService = soldeService;
        this.congeService = congeService;
    }

    public Long employeId(String email) {
        return employe(email).getId();
    }

    public SalarieProfilDto profil(String email) {
        EmployeEmbauche employe = employe(email);
        return new SalarieProfilDto(
                employe.getMatricule(),
                employe.getNomComplet(),
                employe.getPoste() != null ? employe.getPoste().getTitre() : null,
                employe.getDepartement() != null ? employe.getDepartement().getNom() : null,
                soldeService.solde(employe.getId()),
                congeService.types(),
                !employe.isDossierMinimal());
    }

    private EmployeEmbauche employe(String email) {
        Utilisateur compte = utilisateurRepository.verrouillerParEmail(email)
                .orElseThrow(() -> ApiException.notFound("Compte introuvable."));
        if (compte.getEmploye() != null) {
            return compte.getEmploye();
        }
        EmployeEmbauche dossier = dossierExistant(compte).orElseGet(() -> creerDossier(compte));
        compte.setEmploye(dossier);
        utilisateurRepository.save(compte);
        return dossier;
    }

    /** Un dossier porte deja cet email : on le reutilise s'il n'est pas deja rattache a un autre compte. */
    private Optional<EmployeEmbauche> dossierExistant(Utilisateur compte) {
        return employeRepository.findByEmailIgnoreCase(compte.getEmail()).map(dossier -> {
            Optional<Utilisateur> autre = utilisateurRepository.findByEmployeId(dossier.getId());
            if (autre.isPresent() && !autre.get().getIdUtilisateur().equals(compte.getIdUtilisateur())) {
                throw ApiException.conflit("DOSSIER_EMAIL_DEJA_LIE",
                        "Un autre compte utilise déjà ce dossier salarié.");
            }
            return dossier;
        });
    }

    private EmployeEmbauche creerDossier(Utilisateur compte) {
        String[] identite = identite(compte);
        EmployeEmbauche dossier = new EmployeEmbauche();
        dossier.setMatricule(prochainMatricule(compte));
        dossier.setPrenom(identite[0]);
        dossier.setNom(identite[1]);
        dossier.setEmail(compte.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        dossier.setDateEntree(LocalDate.now());
        dossier.setDateEmbauche(LocalDate.now());
        dossier.setStatut(StatutEmploye.ACTIF);
        dossier.setDossierMinimal(true);
        return employeRepository.save(dossier);
    }

    private String prochainMatricule(Utilisateur compte) {
        String base = "AUTO-" + compte.getIdUtilisateur();
        String matricule = base;
        int suffixe = 2;
        while (employeRepository.findByMatricule(matricule).isPresent()) {
            matricule = base + "-" + suffixe++;
        }
        return matricule;
    }

    private static String[] identite(Utilisateur compte) {
        String nomComplet = compte.getNom() != null ? compte.getNom().trim() : "";
        if (nomComplet.isBlank()) {
            nomComplet = compte.getEmail() != null ? compte.getEmail().split("@")[0] : "Utilisateur";
        }
        String[] morceaux = nomComplet.split("\\s+", 2);
        String prenom = morceaux[0];
        String nom = morceaux.length > 1 ? morceaux[1] : morceaux[0];
        return new String[]{prenom, nom};
    }
}
