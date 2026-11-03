package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.Affectation;
import com.mrtovo.backend.entity.AvantageOffre;
import com.mrtovo.backend.entity.CandidatureSelection;
import com.mrtovo.backend.entity.Contrat;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.HistoriqueNegociation;
import com.mrtovo.backend.entity.HistoriqueProposition;
import com.mrtovo.backend.entity.Negociation;
import com.mrtovo.backend.entity.PropositionOffre;
import com.mrtovo.backend.repository.AffectationRepository;
import com.mrtovo.backend.repository.AvantageOffreRepository;
import com.mrtovo.backend.repository.ContratRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.HistoriqueNegociationRepository;
import com.mrtovo.backend.repository.HistoriquePropositionRepository;
import com.mrtovo.backend.repository.NegociationRepository;
import com.mrtovo.backend.dto.AffectationDto;
import com.mrtovo.backend.dto.AvantageDto;
import com.mrtovo.backend.dto.CandidatureSelectionDto;
import com.mrtovo.backend.dto.ContratDto;
import com.mrtovo.backend.dto.EmployeDto;
import com.mrtovo.backend.dto.GrilleDto;
import com.mrtovo.backend.dto.HistoriqueDto;
import com.mrtovo.backend.dto.NegociationDto;
import com.mrtovo.backend.dto.PropositionDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class RecrutementMapper {

    private final AvantageOffreRepository avantageRepository;
    private final NegociationRepository negociationRepository;
    private final HistoriquePropositionRepository historiquePropositionRepository;
    private final HistoriqueNegociationRepository historiqueNegociationRepository;
    private final EmployeEmbaucheRepository employeRepository;
    private final ContratRepository contratRepository;
    private final AffectationRepository affectationRepository;
    private final GrilleSalarialeService grilleSalarialeService;

    public RecrutementMapper(AvantageOffreRepository avantageRepository,
                       NegociationRepository negociationRepository,
                       HistoriquePropositionRepository historiquePropositionRepository,
                       HistoriqueNegociationRepository historiqueNegociationRepository,
                       EmployeEmbaucheRepository employeRepository,
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
        CandidatureSelection candidature = proposition.getCandidature();
        Long posteId = proposition.getPoste() != null ? proposition.getPoste().getId() : null;
        GrilleDto grille = grilleSalarialeService.niveauPourSalaire(posteId, proposition.getSalaireBase(),
                LocalDate.now());
        Boolean dansLaGrille = grille != null
                ? Boolean.TRUE
                : !grilleSalarialeService.postePossedeUneGrille(posteId);
        EmployeEmbauche employe = employeRepository.findByPropositionId(proposition.getId()).orElse(null);

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

    public EmployeDto toEmployeDto(EmployeEmbauche employe) {
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
        EmployeEmbauche employe = contrat.getEmploye();
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

    public CandidatureSelectionDto toCandidatureDto(CandidatureSelection candidature, PropositionOffre proposition) {
        return new CandidatureSelectionDto(
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
