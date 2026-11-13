package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.ActivationCompteSalarieRequest;
import com.mrtovo.backend.dto.ChangementMotDePasseRequest;
import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Bloc 7 : comptes de connexion des salaries. */
@Service
@Transactional
public class CompteSalarieService {

    private static final int OCTETS_MAX_BCRYPT = 72;

    private final UtilisateurRepository utilisateurRepository;
    private final EmployeEmbaucheRepository employeRepository;
    private final PasswordEncoder encodeur;
    private final TraceTempsService trace;

    public CompteSalarieService(UtilisateurRepository utilisateurRepository,
                                EmployeEmbaucheRepository employeRepository,
                                PasswordEncoder encodeur,
                                TraceTempsService trace) {
        this.utilisateurRepository = utilisateurRepository;
        this.employeRepository = employeRepository;
        this.encodeur = encodeur;
        this.trace = trace;
    }

    /**
     * Le mot de passe initial est transmis par le RH. Un salarie venu du recrutement a deja
     * un compte candidat avec le meme email : ce compte est converti plutot que duplique.
     */
    public void creerCompte(Long employeId, String motDePasse, String auteur) {
        verifierLongueur(motDePasse);
        EmployeEmbauche employe = employeRepository.findById(employeId)
                .orElseThrow(() -> ApiException.notFound("Salarié introuvable : " + employeId));
        if (utilisateurRepository.findByEmployeId(employeId).isPresent()) {
            throw ApiException.conflit("COMPTE_EXISTANT", "Ce salarié a déjà un compte.");
        }
        creer(employe, motDePasse, auteur, "");
    }

    /**
     * Le salarie active lui-meme son compte, comme le candidat au Bloc 2.
     * Matricule et email doivent designer le meme dossier ; le message d'erreur ne dit pas lequel est faux.
     */
    public void activer(ActivationCompteSalarieRequest requete) {
        verifierLongueur(requete.motDePasse());
        String email = requete.email().trim().toLowerCase(Locale.ROOT);
        EmployeEmbauche employe = employeRepository.findByMatricule(requete.matricule().trim().toUpperCase(Locale.ROOT))
                .filter(dossier -> dossier.getEmail() != null && dossier.getEmail().trim().equalsIgnoreCase(email))
                .orElseThrow(() -> ApiException.badRequest("IDENTITE_INVALIDE",
                        "Matricule ou email incorrect : vérifiez les informations de votre dossier."));
        if (employe.getStatut() == StatutEmploye.SORTI) {
            throw ApiException.conflit("SALARIE_SORTI", "Ce dossier correspond à un salarié sorti des effectifs.");
        }
        if (utilisateurRepository.findByEmployeId(employe.getId()).isPresent()) {
            throw ApiException.conflit("COMPTE_EXISTANT", "Votre compte existe déjà : connectez-vous.");
        }
        creer(employe, requete.motDePasse(), email, " (activé par le salarié)");
    }

    private void creer(EmployeEmbauche employe, String motDePasse, String auteur, String precision) {

        String email = employe.getEmail().trim().toLowerCase(Locale.ROOT);
        Utilisateur compte = utilisateurRepository.findByEmailIgnoreCase(email).orElseGet(Utilisateur::new);
        boolean conversion = compte.getIdUtilisateur() != null;
        if (conversion && !estSonCompteCandidat(compte, employe)) {
            throw ApiException.conflit("EMAIL_DEJA_UTILISE", "L'email " + email + " sert déjà à un autre compte.");
        }

        compte.setNom(employe.getNomComplet());
        compte.setEmail(email);
        compte.setMotDePasse(encodeur.encode(motDePasse));
        compte.setRole(RoleUtilisateur.EMPLOYE);
        compte.setEmploye(employe);
        utilisateurRepository.save(compte);

        trace.tracer(employe, ActionAudit.CREATION_COMPTE_SALARIE, auteur,
                email + (conversion ? " (compte candidat converti)" : "") + precision);
    }

    public void changerMotDePasse(String email, ChangementMotDePasseRequest requete) {
        verifierLongueur(requete.nouveauMotDePasse());
        Utilisateur compte = utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Compte introuvable."));
        if (!encodeur.matches(requete.ancienMotDePasse(), compte.getMotDePasse())) {
            throw ApiException.badRequest("MOT_DE_PASSE_INCORRECT", "L'ancien mot de passe est incorrect.");
        }
        compte.setMotDePasse(encodeur.encode(requete.nouveauMotDePasse()));
    }

    private static boolean estSonCompteCandidat(Utilisateur compte, EmployeEmbauche employe) {
        return compte.getRole() == RoleUtilisateur.CANDIDAT
                && compte.getCandidat() != null
                && employe.getCandidat() != null
                && compte.getCandidat().getId().equals(employe.getCandidat().getId());
    }

    /** Au-dela de 72 octets, BCrypt refuse ou tronque le mot de passe selon les versions. */
    private static void verifierLongueur(String motDePasse) {
        if (motDePasse.getBytes(StandardCharsets.UTF_8).length > OCTETS_MAX_BCRYPT) {
            throw ApiException.badRequest("MOT_DE_PASSE_TROP_LONG", "Le mot de passe dépasse 72 octets.");
        }
    }
}
