package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.CandidatRecrutement;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.model.Candidat;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompteSalarieServiceTest {

    private UtilisateurRepository utilisateurRepository;
    private EmployeEmbaucheRepository employeRepository;
    private CompteSalarieService service;
    private EmployeEmbauche employe;
    private final BCryptPasswordEncoder encodeur = new BCryptPasswordEncoder(4);

    @BeforeEach
    void preparer() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        employeRepository = mock(EmployeEmbaucheRepository.class);
        service = new CompteSalarieService(utilisateurRepository, employeRepository, encodeur,
                mock(TraceTempsService.class));

        CandidatRecrutement candidat = mock(CandidatRecrutement.class);
        when(candidat.getId()).thenReturn(7L);
        employe = new EmployeEmbauche();
        employe.setId(1L);
        employe.setNom("Rabe");
        employe.setPrenom("Aina");
        employe.setEmail("Aina@mail.mg");
        employe.setCandidat(candidat);
        employe.setMatricule("EMP-TEST-0001");
        employe.setStatut(com.mrtovo.backend.entity.StatutEmploye.ACTIF);
        when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(employeRepository.findByMatricule("EMP-TEST-0001")).thenReturn(Optional.of(employe));
        when(utilisateurRepository.findByEmployeId(1L)).thenReturn(Optional.empty());
        when(utilisateurRepository.findByEmailIgnoreCase("aina@mail.mg")).thenReturn(Optional.empty());
    }

    @Test
    void creeUnCompteSalarieAvecUnMotDePasseChiffre() {
        service.creerCompte(1L, "motdepasse", "rh@rh.com");

        org.mockito.ArgumentCaptor<Utilisateur> compte = org.mockito.ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(compte.capture());
        assertEquals(RoleUtilisateur.EMPLOYE, compte.getValue().getRole());
        assertEquals("aina@mail.mg", compte.getValue().getEmail());
        assertEquals(employe, compte.getValue().getEmploye());
        assertTrue(encodeur.matches("motdepasse", compte.getValue().getMotDePasse()));
    }

    @Test
    void convertitLeCompteCandidatDuSalarieAuLieuDeLeDupliquer() {
        Candidat candidat = new Candidat();
        candidat.setId(7L);
        Utilisateur compteCandidat = new Utilisateur();
        compteCandidat.setIdUtilisateur(50L);
        compteCandidat.setRole(RoleUtilisateur.CANDIDAT);
        compteCandidat.setCandidat(candidat);
        when(utilisateurRepository.findByEmailIgnoreCase("aina@mail.mg")).thenReturn(Optional.of(compteCandidat));

        service.creerCompte(1L, "motdepasse", "rh@rh.com");

        assertEquals(RoleUtilisateur.EMPLOYE, compteCandidat.getRole());
        assertEquals(employe, compteCandidat.getEmploye());
    }

    @Test
    void refuseUnEmailDejaPrisParUnAutreCompte() {
        Utilisateur compteRh = new Utilisateur();
        compteRh.setIdUtilisateur(2L);
        compteRh.setRole(RoleUtilisateur.RH);
        when(utilisateurRepository.findByEmailIgnoreCase("aina@mail.mg")).thenReturn(Optional.of(compteRh));

        ApiException erreur = assertThrows(ApiException.class, () -> service.creerCompte(1L, "motdepasse", "rh@rh.com"));

        assertEquals("EMAIL_DEJA_UTILISE", erreur.getCode());
        assertEquals(RoleUtilisateur.RH, compteRh.getRole());
    }

    @Test
    void refuseUnDeuxiemeCompte() {
        when(utilisateurRepository.findByEmployeId(1L)).thenReturn(Optional.of(new Utilisateur()));

        ApiException erreur = assertThrows(ApiException.class, () -> service.creerCompte(1L, "motdepasse", "rh@rh.com"));

        assertEquals("COMPTE_EXISTANT", erreur.getCode());
    }

    @Test
    void changerSonMotDePasseExigeLAncien() {
        Utilisateur compte = new Utilisateur();
        compte.setMotDePasse(encodeur.encode("ancienmdp"));
        when(utilisateurRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.of(compte));

        ApiException erreur = assertThrows(ApiException.class, () -> service.changerMotDePasse("aina@mail.mg",
                new com.mrtovo.backend.dto.ChangementMotDePasseRequest("faux", "nouveaumdp")));
        assertEquals("MOT_DE_PASSE_INCORRECT", erreur.getCode());

        service.changerMotDePasse("aina@mail.mg",
                new com.mrtovo.backend.dto.ChangementMotDePasseRequest("ancienmdp", "nouveaumdp"));
        assertTrue(encodeur.matches("nouveaumdp", compte.getMotDePasse()));
    }

    // ===================================================== Activation par le salarie lui-meme

    @Test
    void leSalarieActiveSonCompteAvecSonMatriculeEtSonEmail() {
        service.activer(new com.mrtovo.backend.dto.ActivationCompteSalarieRequest(" emp-test-0001 ", "AINA@mail.mg",
                "motdepasse"));

        org.mockito.ArgumentCaptor<Utilisateur> compte = org.mockito.ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(compte.capture());
        assertEquals(RoleUtilisateur.EMPLOYE, compte.getValue().getRole());
        assertEquals(employe, compte.getValue().getEmploye());
        assertTrue(encodeur.matches("motdepasse", compte.getValue().getMotDePasse()));
    }

    @Test
    void unMauvaisEmailOuUnMauvaisMatriculeDonneLaMemeErreur() {
        ApiException mauvaisEmail = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> service.activer(new com.mrtovo.backend.dto.ActivationCompteSalarieRequest("EMP-TEST-0001",
                        "autre@mail.mg", "motdepasse")));
        ApiException mauvaisMatricule = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> service.activer(new com.mrtovo.backend.dto.ActivationCompteSalarieRequest("EMP-9999",
                        "aina@mail.mg", "motdepasse")));

        assertEquals("IDENTITE_INVALIDE", mauvaisEmail.getCode());
        assertEquals(mauvaisEmail.getMessage(), mauvaisMatricule.getMessage(),
                "le message ne doit pas dire lequel des deux est faux");
        verify(utilisateurRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onNActivePasDeuxFoisLeMemeCompte() {
        when(utilisateurRepository.findByEmployeId(1L)).thenReturn(Optional.of(new Utilisateur()));

        ApiException erreur = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> service.activer(new com.mrtovo.backend.dto.ActivationCompteSalarieRequest("EMP-TEST-0001",
                        "aina@mail.mg", "motdepasse")));
        assertEquals("COMPTE_EXISTANT", erreur.getCode());
    }

    @Test
    void unSalarieSortiNePeutPasActiverDeCompte() {
        employe.setStatut(com.mrtovo.backend.entity.StatutEmploye.SORTI);

        ApiException erreur = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> service.activer(new com.mrtovo.backend.dto.ActivationCompteSalarieRequest("EMP-TEST-0001",
                        "aina@mail.mg", "motdepasse")));
        assertEquals("SALARIE_SORTI", erreur.getCode());
    }
}
