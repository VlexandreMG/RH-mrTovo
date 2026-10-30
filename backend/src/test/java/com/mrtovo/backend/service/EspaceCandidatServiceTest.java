package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.PropositionOffre;
import com.mrtovo.backend.entity.StatutProposition;
import com.mrtovo.backend.entity.TypeNegociation;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.PropositionOffreRepository;
import com.mrtovo.backend.dto.RevendicationRequest;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.model.Candidat;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspaceCandidatServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PropositionOffreRepository propositionRepository;
    @Mock
    private PropositionService propositionService;
    @Mock
    private NegociationService negociationService;
    @Mock
    private RecrutementMapper mapper;
    @InjectMocks
    private EspaceCandidatService service;

    @Test
    void neRetourneQueLesPropositionsVisiblesDuCompteAssocie() {
        Candidat candidat = new Candidat();
        candidat.setId(31L);
        Utilisateur utilisateur = candidatUtilisateur(candidat);
        PropositionOffre emise = proposition(StatutProposition.EMISE);
        PropositionOffre brouillon = proposition(StatutProposition.BROUILLON);
        when(utilisateurRepository.findByEmailIgnoreCase("candidate@example.com")).thenReturn(Optional.of(utilisateur));
        when(propositionRepository.findByCandidature_Candidat_IdOrderByDateEmissionDesc(31L))
                .thenReturn(List.of(emise, brouillon));
        when(mapper.toPropositionDto(emise)).thenReturn(null);

        List<?> result = service.propositions("candidate@example.com");

        assertEquals(1, result.size());
        verify(mapper).toPropositionDto(emise);
        verify(mapper, never()).toPropositionDto(brouillon);
    }

    @Test
    void refuseLesComptesSansProfilCandidatAssocie() {
        when(utilisateurRepository.findByEmailIgnoreCase("candidate@example.com"))
                .thenReturn(Optional.of(candidatUtilisateur(null)));

        ApiException error = assertThrows(ApiException.class,
                () -> service.propositions("candidate@example.com"));

        assertEquals(403, error.getStatus().value());
        verify(propositionRepository, never()).findByCandidature_Candidat_IdOrderByDateEmissionDesc(1L);
    }

    @Test
    void nePermetPasUneActionSurLaPropositionDUnAutreCandidat() {
        Candidat candidat = new Candidat();
        candidat.setId(31L);
        when(utilisateurRepository.findByEmailIgnoreCase("candidate@example.com"))
                .thenReturn(Optional.of(candidatUtilisateur(candidat)));
        when(propositionRepository.findByIdAndCandidature_Candidat_Id(99L, 31L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> service.accepter("candidate@example.com", 99L, null));

        assertEquals(404, error.getStatus().value());
        verify(propositionService, never()).accepter(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void forceUneRevendicationCandidateEtRefuseUneDemandeVide() {
        Candidat candidat = new Candidat();
        candidat.setId(31L);
        when(utilisateurRepository.findByEmailIgnoreCase("candidate@example.com"))
                .thenReturn(Optional.of(candidatUtilisateur(candidat)));
        PropositionOffre emise = proposition(StatutProposition.EMISE);
        emise.setDateLimiteValidite(LocalDate.now().plusDays(2));
        when(propositionRepository.findByIdAndCandidature_Candidat_Id(10L, 31L)).thenReturn(Optional.of(emise));

        assertThrows(ApiException.class, () -> service.revendiquer("candidate@example.com", 10L,
                new RevendicationRequest(null, TypeNegociation.REVENDICATION, null, " ", "Motif")));
        verify(negociationService, never()).deposerRevendication(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());

        service.revendiquer("candidate@example.com", 10L,
                new RevendicationRequest(null, TypeNegociation.CONTRE_PROPOSITION,
                        new BigDecimal("1000"), null, "Motif"));

        verify(negociationService).deposerRevendication(eq(10L), argThat(request ->
                request.acteur() == com.mrtovo.backend.entity.Acteur.CANDIDAT
                        && request.type() == TypeNegociation.REVENDICATION));
    }

    private Utilisateur candidatUtilisateur(Candidat candidat) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("candidate@example.com");
        utilisateur.setRole(RoleUtilisateur.CANDIDAT);
        utilisateur.setCandidat(candidat);
        return utilisateur;
    }

    private PropositionOffre proposition(StatutProposition statut) {
        PropositionOffre proposition = new PropositionOffre();
        proposition.setStatut(statut);
        return proposition;
    }
}
