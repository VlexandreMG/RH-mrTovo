package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.StatutCandidatureRecrutement;
import com.mrtovo.backend.entity.StatutContrat;
import com.mrtovo.backend.entity.StatutNegociation;
import com.mrtovo.backend.entity.StatutProposition;
import com.mrtovo.backend.repository.ContratRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.NegociationRepository;
import com.mrtovo.backend.repository.PropositionOffreRepository;
import com.mrtovo.backend.dto.DashboardDto;
import com.mrtovo.backend.dto.PropositionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DashboardService {

    private final PropositionOffreRepository propositionRepository;
    private final NegociationRepository negociationRepository;
    private final EmployeEmbaucheRepository employeRepository;
    private final ContratRepository contratRepository;
    private final CandidatureService candidatureService;
    private final RecrutementMapper mapper;

    public DashboardService(PropositionOffreRepository propositionRepository,
                            NegociationRepository negociationRepository,
                            EmployeEmbaucheRepository employeRepository,
                            ContratRepository contratRepository,
                            CandidatureService candidatureService,
                            RecrutementMapper mapper) {
        this.propositionRepository = propositionRepository;
        this.negociationRepository = negociationRepository;
        this.employeRepository = employeRepository;
        this.contratRepository = contratRepository;
        this.candidatureService = candidatureService;
        this.mapper = mapper;
    }

    public DashboardDto tableauDeBord() {
        Map<String, Long> repartitionPropositions = new java.util.LinkedHashMap<>();
        for (StatutProposition statut : StatutProposition.values()) {
            repartitionPropositions.put(statut.name(), propositionRepository.countByStatut(statut));
        }
        Map<String, Long> repartitionNegociations = new java.util.LinkedHashMap<>();
        for (StatutNegociation statut : StatutNegociation.values()) {
            repartitionNegociations.put(statut.name(), negociationRepository.countByStatutIn(List.of(statut)));
        }

        long propositionsEnCours = propositionRepository.countByStatut(StatutProposition.BROUILLON)
                + propositionRepository.countByStatut(StatutProposition.VALIDEE)
                + propositionRepository.countByStatut(StatutProposition.EMISE)
                + propositionRepository.countByStatut(StatutProposition.EN_NEGOCIATION)
                + propositionRepository.countByStatut(StatutProposition.REVISE);

        List<PropositionDto> dernieres = propositionRepository.findAllByOrderByDateEmissionDesc().stream()
                .limit(8)
                .map(mapper::toPropositionDto)
                .toList();

        long candidaturesRetenues = candidatureService.listerRetenues().size();
        long negotiationsEnCours = negociationRepository.countByStatutIn(List.of(
                StatutNegociation.EN_ATTENTE, StatutNegociation.EN_ANALYSE, StatutNegociation.CONTRE_PROPOSITION));

        return new DashboardDto(
                candidaturesRetenues,
                propositionsEnCours,
                propositionRepository.countByStatut(StatutProposition.EMISE),
                propositionRepository.countByStatut(StatutProposition.ACCEPTEE),
                negotiationsEnCours,
                employeRepository.count(),
                contratRepository.countByStatutContrat(StatutContrat.A_SIGNER),
                repartitionPropositions,
                repartitionNegociations,
                dernieres);
    }

    public long compterCandidatures(StatutCandidatureRecrutement statut) {
        return candidatureService.listerPipeline().stream()
                .filter(candidature -> candidature.statut() == statut)
                .count();
    }
}
