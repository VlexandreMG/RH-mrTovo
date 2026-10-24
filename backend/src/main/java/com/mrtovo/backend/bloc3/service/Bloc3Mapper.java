package com.mrtovo.backend.bloc3.service;

import com.mrtovo.backend.bloc3.domain.Affectation;
import com.mrtovo.backend.bloc3.domain.AvantageOffre;
import com.mrtovo.backend.bloc3.domain.Candidature;
import com.mrtovo.backend.bloc3.domain.Contrat;
import com.mrtovo.backend.bloc3.domain.Employe;
import com.mrtovo.backend.bloc3.domain.HistoriqueNegociation;
import com.mrtovo.backend.bloc3.domain.HistoriqueProposition;
import com.mrtovo.backend.bloc3.domain.Negociation;
import com.mrtovo.backend.bloc3.domain.PropositionOffre;
import com.mrtovo.backend.bloc3.repository.AffectationRepository;
import com.mrtovo.backend.bloc3.repository.AvantageOffreRepository;
import com.mrtovo.backend.bloc3.repository.ContratRepository;
import com.mrtovo.backend.bloc3.repository.EmployeRepository;
import com.mrtovo.backend.bloc3.repository.HistoriqueNegociationRepository;
import com.mrtovo.backend.bloc3.repository.HistoriquePropositionRepository;
import com.mrtovo.backend.bloc3.repository.NegociationRepository;
import com.mrtovo.backend.bloc3.web.dto.AffectationDto;
import com.mrtovo.backend.bloc3.web.dto.AvantageDto;
import com.mrtovo.backend.bloc3.web.dto.CandidatureDto;
import com.mrtovo.backend.bloc3.web.dto.ContratDto;
import com.mrtovo.backend.bloc3.web.dto.EmployeDto;
import com.mrtovo.backend.bloc3.web.dto.GrilleDto;
import com.mrtovo.backend.bloc3.web.dto.HistoriqueDto;
import com.mrtovo.backend.bloc3.web.dto.NegociationDto;
import com.mrtovo.backend.bloc3.web.dto.PropositionDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class Bloc3Mapper {

    private final AvantageOffreRepository avantageRepository;
    private final NegociationRepository negociationRepository;
    private final HistoriquePropositionRepository historiquePropositionRepository;
    private final HistoriqueNegociationRepository historiqueNegociationRepository;
    private final EmployeRepository employeRepository;
    private final ContratRepository contratRepository;
    private final AffectationRepository affectationRepository;
    private final GrilleSalarialeService grilleSalarialeService;

    public Bloc3Mapper(AvantageOffreRepository avantageRepository,
                       NegociationRepository negociationRepository,
                       HistoriquePropositionRepository historiquePropositionRepository,
                       HistoriqueNegociationRepository historiqueNegociationRepository,
                       EmployeRepository employeRepository,
                       ContratRepository contratRepository,
                       AffectationRepository affectationRepository,
                       GrilleSalarialeService grilleSalarialeService) {
        this.avantageRepository = avantageRepository;
        this.negociationRepository = negociationRepository;
        this.historiquePropositionRepository = historiquePropositionRepository;
        this.historiqueNegociationRepository = historiqueNegociationRepository;
        this.employeRepository = employeRepository;
        this.contratRepository = contratRepository;
        this.affectationRepository = affectationRepository;
        this.grilleSalarialeService = grilleSalarialeService;
    }

    public PropositionDto toPropositionDto(PropositionOffre proposition) {
        Candidature candidature = proposition.getCandidature();
        Long posteId = proposition.getPoste() != null ? proposition.getPoste().getId() : null;
        GrilleDto grille = grilleSalarialeService.niveauPourSalaire(posteId, proposition.getSalaireBase(),
                LocalDate.now());
        Boolean dansLaGrille = grille != null
                ? Boolean.TRUE
                : !grilleSalarialeService.postePossedeUneGrille(posteId);
        Employe employe = employeRepository.findByPropositionId(proposition.getId()).orElse(null);

        return new PropositionDto(
                proposition.getId(),
                candidature != null ? candidature.getId() : null,
                candidature != null && candidature.getCandidat() != null ? candidature.getCandidat().getNom() : null,
                candidature != null && candidature.getCandidat() != null ? candidature.getCandidat().getPrenom() : null,
                candidature != null && candidature.getCandidat() != null ? candidature.getCandidat().getEmail() : null,
                candidature != null && candidature.getCandidat() != null ? candidature.getCandidat().getTelephone() : null,
                candidature != null && candidature.getOffreEmploi() != null ? candidature.getOffreEmploi().getId() : null,
                candidature != null && candidature.getOffreEmploi() != null ? candidature.getOffreEmploi().getTitre() : null,
                proposition.getVersion(),
                proposition.getPropositionParent() != null ? proposition.getPropositionParent().getId() : null,
                proposition.getSalaireBase(),
                proposition.getTempsTravail(),
                proposition.getEmploiDuTemps(),
                proposition.getLieuTravail(),
                proposition.getDatePrisePoste(),
                proposition.getDateLimiteValidite(),
                proposition.getDateEmission(),
                proposition.getDateFinContrat(),
                proposition.getTypeContrat(),
                proposition.getPeriodeEssaiMois(),
                posteId,
                proposition.getPoste() != null ? proposition.getPoste().getTitre() : null,
                proposition.getDepartement() != null ? proposition.getDepartement().getId() : null,
                proposition.getDepartement() != null ? proposition.getDepartement().getNom() : null,
                proposition.getStatut(),
                proposition.getMotifRevision(),
                proposition.getValideePar(),
                proposition.getDateValidation(),
                proposition.getDateAcceptation(),
                avantageRepository.findByPropositionIdOrderByOrdreAsc(proposition.getId()).stream()
                        .map(this::toAvantageDto).toList(),
                negociationRepository.findByPropositionIdOrderByDateDemandeDesc(proposition.getId()).stream()
                        .map(this::toNegociationDto).toList(),
                historiquePropositionRepository.findByPropositionIdOrderByDateChangementAsc(proposition.getId()).stream()
                        .map(this::toHistoriqueDto).toList(),
                grille,
                dansLaGrille,
                employe != null ? employe.getId() : null,
                employe != null ? employe.getMatricule() : null);
    }

    public List<PropositionDto> toPropositionDtos(List<PropositionOffre> propositions) {
        return propositions.stream().map(this::toPropositionDto).toList();
    }

    public AvantageDto toAvantageDto(AvantageOffre avantage) {
        return new AvantageDto(avantage.getId(), avantage.getType(), avantage.getLibelle(),
                avantage.getDescription(), avantage.getMontant(), avantage.getOrdre());
    }

    public NegociationDto toNegociationDto(Negociation negociation) {
        return new NegociationDto(
                negociation.getId(),
                negociation.getProposition() != null ? negociation.getProposition().getId() : null,
                negociation.getProposition() != null ? negociation.getProposition().getVersion() : null,
                negociation.getCandidat() != null ? negociation.getCandidat().getId() : null,
                negociation.getCandidat() != null ? negociation.getCandidat().getNom() : null,
                negociation.getCandidat() != null ? negociation.getCandidat().getPrenom() : null,
                negociation.getType(),
                negociation.getSalaireRevendique(),
                negociation.getAvantageRevendique(),
                negociation.getMotif(),
                negociation.getDateDemande(),
                negociation.getStatut(),
                negociation.getDecidePar(),
                negociation.getDateDecision(),
                negociation.getCommentaireDecision(),
                negociation.getPropositionResultante() != null ? negociation.getPropositionResultante().getId() : null,
                historiqueNegociationRepository.findByNegociationIdOrderByDateChangementAsc(negociation.getId()).stream()
                        .map(this::toHistoriqueDto).toList());
    }

    public HistoriqueDto toHistoriqueDto(HistoriqueProposition historique) {
        return new HistoriqueDto(historique.getId(), historique.getAncienStatut(), historique.getNouveauStatut(),
                historique.getAction(), historique.getAuteur(), historique.getCommentaire(),
                historique.getDateChangement());
    }

    public HistoriqueDto toHistoriqueDto(HistoriqueNegociation historique) {
        return new HistoriqueDto(historique.getId(), historique.getAncienStatut(), historique.getNouveauStatut(),
                historique.getAction(), historique.getAuteur(), historique.getCommentaire(),
                historique.getDateChangement());
    }

    public EmployeDto toEmployeDto(Employe employe) {
        return new EmployeDto(
                employe.getId(),
                employe.getMatricule(),
                employe.getCandidat() != null ? employe.getCandidat().getId() : null,
                employe.getProposition() != null ? employe.getProposition().getId() : null,
                employe.getNom(),
                employe.getPrenom(),
                employe.getEmail(),
                employe.getTelephone(),
                employe.getAdresse(),
                employe.getPoste() != null ? employe.getPoste().getId() : null,
                employe.getPoste() != null ? employe.getPoste().getTitre() : null,
                employe.getDepartement() != null ? employe.getDepartement().getId() : null,
                employe.getDepartement() != null ? employe.getDepartement().getNom() : null,
                employe.getDateEmbauche(),
                employe.getDateEntree(),
                employe.getDateFinContrat(),
                employe.getDateFinPeriodeEssai(),
                employe.getStatut(),
                contratRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId()).stream()
                        .map(this::toContratDto).toList(),
                affectationRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId()).stream()
                        .map(this::toAffectationDto).toList());
    }

    public ContratDto toContratDto(Contrat contrat) {
        Employe employe = contrat.getEmploye();
        return new ContratDto(
                contrat.getId(),
                employe != null ? employe.getId() : null,
                employe != null ? employe.getNomComplet() : null,
                employe != null ? employe.getMatricule() : null,
                contrat.getReference(),
                contrat.getTypeContrat(),
                contrat.getDateDebut(),
                contrat.getDateFin(),
                contrat.getSalaire(),
                contrat.getStatutContrat(),
                contrat.getDocumentUrl());
    }

    public AffectationDto toAffectationDto(Affectation affectation) {
        return new AffectationDto(
                affectation.getId(),
                affectation.getEmploye() != null ? affectation.getEmploye().getId() : null,
                affectation.getPoste() != null ? affectation.getPoste().getId() : null,
                affectation.getPoste() != null ? affectation.getPoste().getTitre() : null,
                affectation.getDepartement() != null ? affectation.getDepartement().getId() : null,
                affectation.getDepartement() != null ? affectation.getDepartement().getNom() : null,
                affectation.getType(),
                affectation.getDateDebut(),
                affectation.getDateFin(),
                affectation.getMotif(),
                affectation.getStatut());
    }

    public CandidatureDto toCandidatureDto(Candidature candidature, PropositionOffre proposition) {
        return new CandidatureDto(
                candidature.getId(),
                candidature.getCandidat() != null ? candidature.getCandidat().getId() : null,
                candidature.getCandidat() != null ? candidature.getCandidat().getNom() : null,
                candidature.getCandidat() != null ? candidature.getCandidat().getPrenom() : null,
                candidature.getCandidat() != null ? candidature.getCandidat().getEmail() : null,
                candidature.getCandidat() != null ? candidature.getCandidat().getTelephone() : null,
                candidature.getOffreEmploi() != null ? candidature.getOffreEmploi().getId() : null,
                candidature.getOffreEmploi() != null ? candidature.getOffreEmploi().getTitre() : null,
                candidature.getDateCandidature(),
                candidature.getStatut(),
                proposition != null ? proposition.getId() : null,
                proposition != null ? proposition.getStatut() : null);
    }
}
