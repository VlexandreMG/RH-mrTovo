package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.CandidatureSelection;
import com.mrtovo.backend.entity.PropositionOffre;
import com.mrtovo.backend.entity.StatutCandidatureRecrutement;
import com.mrtovo.backend.repository.CandidatureSelectionRepository;
import com.mrtovo.backend.repository.PropositionOffreRepository;
import com.mrtovo.backend.dto.CandidatureSelectionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class CandidatureService {

    private static final List<StatutCandidatureRecrutement> STATUTS_BLOC3 = List.of(
            StatutCandidatureRecrutement.RETENU,
            StatutCandidatureRecrutement.PROPOSITION_ENVOYEE,
            StatutCandidatureRecrutement.EN_NEGOCIATION,
            StatutCandidatureRecrutement.EMBAUCHE);

    private final CandidatureSelectionRepository candidatureRepository;
    private final PropositionOffreRepository propositionRepository;
    private final RecrutementMapper mapper;

    public CandidatureService(CandidatureSelectionRepository candidatureRepository,
                              PropositionOffreRepository propositionRepository,
                              RecrutementMapper mapper) {
        this.candidatureRepository = candidatureRepository;
        this.propositionRepository = propositionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CandidatureSelectionDto> listerPipeline() {
        return candidatureRepository.findByStatutCandidatureIdInOrderByDateCandidatureDesc(
                        STATUTS_BLOC3.stream().map(this::statutId).toList()).stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidatureSelectionDto> listerRetenues() {
        return candidatureRepository.findByStatutCandidatureIdInOrderByDateCandidatureDesc(
                        List.of(statutId(StatutCandidatureRecrutement.RETENU))).stream()
                .map(this::convertir)
                .toList();
    }

    private CandidatureSelectionDto convertir(CandidatureSelection candidature) {
        PropositionOffre derniere = propositionRepository
                .findByCandidatureIdOrderByVersionAsc(candidature.getId()).stream()
                .max(Comparator.comparing(PropositionOffre::getVersion))
                .orElse(null);
        return mapper.toCandidatureDto(candidature, derniere);
    }

    private long statutId(StatutCandidatureRecrutement statut) {
        return switch (statut) {
            case EN_ATTENTE -> 1L;
            case EN_COURS_EVALUATION -> 2L;
            case EN_ENTRETIEN -> 3L;
            case RETENU, PROPOSITION_ENVOYEE, EN_NEGOCIATION, EMBAUCHE -> 4L;
            case REFUSE, ABANDONNE -> 5L;
        };
    }
}
