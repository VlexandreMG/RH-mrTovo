package com.mrtovo.backend.bloc3.service;

import com.mrtovo.backend.bloc3.domain.StatutCandidature;
import com.mrtovo.backend.bloc3.domain.StatutContrat;
import com.mrtovo.backend.bloc3.domain.StatutNegociation;
import com.mrtovo.backend.bloc3.domain.StatutProposition;
import com.mrtovo.backend.bloc3.repository.ContratRepository;
import com.mrtovo.backend.bloc3.repository.EmployeRepository;
import com.mrtovo.backend.bloc3.repository.NegociationRepository;
import com.mrtovo.backend.bloc3.repository.PropositionOffreRepository;
import com.mrtovo.backend.bloc3.web.dto.DashboardDto;
import com.mrtovo.backend.bloc3.web.dto.PropositionDto;
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
    private final EmployeRepository employeRepository;
    private final ContratRepository contratRepository;
    private final CandidatureService candidatureService;
    private final Bloc3Mapper mapper;

    public DashboardService(PropositionOffreRepository propositionRepository,
                            NegociationRepository negociationRepository,
                            EmployeRepository employeRepository,
                            ContratRepository contratRepository,
                            CandidatureService candidatureService,
                            Bloc3Mapper mapper) {
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

    public long compterCandidatures(StatutCandidature statut) {
        return candidatureService.listerPipeline().stream()
                .filter(candidature -> candidature.statut() == statut)
                .count();
    }
}
