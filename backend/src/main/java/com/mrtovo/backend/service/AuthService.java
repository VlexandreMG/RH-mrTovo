package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.AuthRequest;
import com.mrtovo.backend.dto.AuthResponse;
import com.mrtovo.backend.dto.CandidatInscriptionRequest;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.model.Candidat;
import com.mrtovo.backend.repository.CandidatRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final CandidatRepository candidatRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UtilisateurRepository utilisateurRepository, CandidatRepository candidatRepository,
                       PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.candidatRepository = candidatRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(AuthRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Utilisateur u = utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
        if (!passwordEncoder.matches(request.motDePasse(), u.getMotDePasse())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        return versReponse(u);
    }

    @Transactional
    public AuthResponse inscrireCandidat(CandidatInscriptionRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(CONFLICT, "Un compte existe déjà avec cette adresse e-mail.");
        }
        if (request.motDePasse().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                    "Le mot de passe ne peut pas dépasser 72 octets UTF-8.");
        }

        Candidat candidat = candidatRepository.findByEmailIgnoreCase(email).orElseGet(Candidat::new);
        candidat.setNom(request.nom().trim());
        candidat.setPrenom(request.prenom().trim());
        candidat.setEmail(email);
        candidat.setTelephone(request.telephone() == null || request.telephone().isBlank()
                ? null : request.telephone().trim());
        candidat = candidatRepository.save(candidat);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(candidat.getPrenom() + " " + candidat.getNom());
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.motDePasse()));
        utilisateur.setRole(RoleUtilisateur.CANDIDAT);
        utilisateur.setCandidat(candidat);
        return versReponse(utilisateurRepository.save(utilisateur));
    }

    private AuthResponse versReponse(Utilisateur utilisateur) {
        return new AuthResponse(utilisateur.getIdUtilisateur(), utilisateur.getNom(), utilisateur.getEmail(),
                utilisateur.getRole().name(),
                utilisateur.getDepartement() == null ? null : utilisateur.getDepartement().getNom());
    }
}
