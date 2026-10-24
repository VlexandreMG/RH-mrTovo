package com.mrtovo.backend.bloc3.service;

import com.mrtovo.backend.bloc3.domain.Candidature;
import com.mrtovo.backend.bloc3.domain.PropositionOffre;
import com.mrtovo.backend.bloc3.domain.StatutCandidature;
import com.mrtovo.backend.bloc3.repository.CandidatureRepository;
import com.mrtovo.backend.bloc3.repository.PropositionOffreRepository;
import com.mrtovo.backend.bloc3.web.dto.CandidatureDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class CandidatureService {

    private static final List<StatutCandidature> STATUTS_BLOC3 = List.of(
            StatutCandidature.RETENU,
            StatutCandidature.PROPOSITION_ENVOYEE,
            StatutCandidature.EN_NEGOCIATION,
            StatutCandidature.EMBAUCHE);

    private final CandidatureRepository candidatureRepository;
    private final PropositionOffreRepository propositionRepository;
    private final Bloc3Mapper mapper;

    public CandidatureService(CandidatureRepository candidatureRepository,
                              PropositionOffreRepository propositionRepository,
                              Bloc3Mapper mapper) {
        this.candidatureRepository = candidatureRepository;
        this.propositionRepository = propositionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CandidatureDto> listerPipeline() {
        return candidatureRepository.findByStatutInOrderByDateCandidatureDesc(STATUTS_BLOC3).stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidatureDto> listerRetenues() {
        return candidatureRepository.findByStatutInOrderByDateCandidatureDesc(
                        List.of(StatutCandidature.RETENU)).stream()
                .map(this::convertir)
                .toList();
    }

    private CandidatureDto convertir(Candidature candidature) {
        PropositionOffre derniere = propositionRepository
                .findByCandidatureIdOrderByVersionAsc(candidature.getId()).stream()
                .max(Comparator.comparing(PropositionOffre::getVersion))
                .orElse(null);
        return mapper.toCandidatureDto(candidature, derniere);
    }
}
