package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.CandidatInscriptionRequest;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.model.Candidat;
import com.mrtovo.backend.repository.CandidatRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private CandidatRepository candidatRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AuthService authService;

    @Test
    void inscritUnCompteCandidatEtLeRelieAuProfilExistant() {
        Candidat candidat = new Candidat();
        candidat.setId(42L);
        when(utilisateurRepository.existsByEmailIgnoreCase("candidat@example.com")).thenReturn(false);
        when(candidatRepository.findByEmailIgnoreCase("candidat@example.com")).thenReturn(Optional.of(candidat));
        when(candidatRepository.save(any(Candidat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur saved = invocation.getArgument(0);
            saved.setIdUtilisateur(7L);
            return saved;
        });

        var response = authService.inscrireCandidat(new CandidatInscriptionRequest(
                " Tovo ", " Alex ", " Candidat@Example.com ", "motdepasse123", " 0340000000 "));

        ArgumentCaptor<Utilisateur> account = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(account.capture());
        assertEquals("candidat@example.com", response.email());
        assertEquals("CANDIDAT", response.role());
        assertEquals("Alex Tovo", account.getValue().getNom());
        assertEquals(RoleUtilisateur.CANDIDAT, account.getValue().getRole());
        assertEquals(candidat, account.getValue().getCandidat());
        assertEquals("hash", account.getValue().getMotDePasse());
        assertEquals("0340000000", candidat.getTelephone());
    }

    @Test
    void refuseUneAdresseDejaUtiliseeParUnCompte() {
        when(utilisateurRepository.existsByEmailIgnoreCase("candidat@example.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> authService.inscrireCandidat(
                new CandidatInscriptionRequest("Tovo", "Alex", "candidat@example.com", "motdepasse123", null)));
    }

    @Test
    void refuseUnMotDePasseQuiDepasseLaLimiteBcryptEnOctets() {
        when(utilisateurRepository.existsByEmailIgnoreCase("candidat@example.com")).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> authService.inscrireCandidat(
                new CandidatInscriptionRequest("Tovo", "Alex", "candidat@example.com",
                        "é".repeat(37), null)));
    }
}
