package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.Acteur;
import com.mrtovo.backend.entity.CandidatureSelection;
import com.mrtovo.backend.entity.Negociation;
import com.mrtovo.backend.entity.PropositionOffre;
import com.mrtovo.backend.entity.StatutCandidatureRecrutement;
import com.mrtovo.backend.entity.StatutNegociation;
import com.mrtovo.backend.entity.StatutProposition;
import com.mrtovo.backend.repository.HistoriqueNegociationRepository;
import com.mrtovo.backend.repository.NegociationRepository;
import com.mrtovo.backend.dto.DecisionNegociationRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NegociationServiceTest {

    @Test
    void refuseUneRevendicationSansRefuserLaPropositionInitiale() {
        NegociationRepository negociationRepository = mock(NegociationRepository.class);
        HistoriqueNegociationRepository historiqueRepository = mock(HistoriqueNegociationRepository.class);
        PropositionService propositionService = mock(PropositionService.class);
        GrilleSalarialeService grilleSalarialeService = mock(GrilleSalarialeService.class);
        RecrutementMapper mapper = mock(RecrutementMapper.class);

        NegociationService service = new NegociationService(negociationRepository, historiqueRepository,
                propositionService, grilleSalarialeService, mapper);

        CandidatureSelection candidature = mock(CandidatureSelection.class);
        PropositionOffre proposition = mock(PropositionOffre.class);
        when(proposition.getId()).thenReturn(12L);
        when(proposition.getStatut()).thenReturn(StatutProposition.EN_NEGOCIATION);
        when(proposition.getCandidature()).thenReturn(candidature);

        Negociation negociation = new Negociation();
        negociation.setId(25L);
        negociation.setStatut(StatutNegociation.EN_ATTENTE);
        negociation.setProposition(proposition);
        when(negociationRepository.findById(25L)).thenReturn(Optional.of(negociation));
        when(propositionService.chargerPourMaj(12L)).thenReturn(proposition);

        service.decider(25L, new DecisionNegociationRequest(
                Acteur.RH, "REFUSE", null, "Salaire non retenu", null, null, null));

        verify(propositionService).transitionner(proposition, StatutProposition.EMISE,
                "REVENDICATION_REFUSEE_OFFRE_MAINTENUE", "RH", "Salaire non retenu");
        verify(propositionService).changerStatutCandidature(candidature, StatutCandidatureRecrutement.PROPOSITION_ENVOYEE);
    }
}
