package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.AuthRequest;
import com.mrtovo.backend.dto.AuthResponse;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(AuthRequest request) {
        Utilisateur u = utilisateurRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
        if (!passwordEncoder.matches(request.motDePasse(), u.getMotDePasse())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        return new AuthResponse(u.getIdUtilisateur(), u.getNom(), u.getEmail(), u.getRole().name(),
        u.getDepartement() == null ? null : u.getDepartement().getNom());
    }
}
