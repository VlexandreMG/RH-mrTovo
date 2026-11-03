package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.PropositionOffre;
import com.mrtovo.backend.entity.StatutProposition;
import com.mrtovo.backend.entity.TypeNegociation;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.PropositionOffreRepository;
import com.mrtovo.backend.dto.AcceptionRequest;
import com.mrtovo.backend.dto.ActionRequest;
import com.mrtovo.backend.dto.NegociationDto;
import com.mrtovo.backend.dto.PropositionDto;
import com.mrtovo.backend.dto.RevendicationRequest;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.mrtovo.backend.entity.Acteur.CANDIDAT;

@Service
@Transactional
public class EspaceCandidatService {

    private final UtilisateurRepository utilisateurRepository;
    private final PropositionOffreRepository propositionRepository;
    private final PropositionService propositionService;
    private final NegociationService negociationService;
    private final RecrutementMapper mapper;

    public EspaceCandidatService(UtilisateurRepository utilisateurRepository,
                                 PropositionOffreRepository propositionRepository,
                                 PropositionService propositionService,
                                 NegociationService negociationService,
                                 RecrutementMapper mapper) {
        this.utilisateurRepository = utilisateurRepository;
        this.propositionRepository = propositionRepository;
        this.propositionService = propositionService;
        this.negociationService = negociationService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PropositionDto> propositions(String email) {
        Long candidatId = candidatId(email);
        return propositionRepository.findByCandidature_Candidat_IdOrderByDateEmissionDesc(candidatId).stream()
                .filter(proposition -> proposition.getStatut() != StatutProposition.BROUILLON
                        && proposition.getStatut() != StatutProposition.VALIDEE
                        && proposition.getStatut() != StatutProposition.REVISE)
                .map(mapper::toPropositionDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropositionDto proposition(String email, Long propositionId) {
        PropositionOffre proposition = chargerPourCandidat(email, propositionId);
        verifierVisible(proposition);
        return mapper.toPropositionDto(proposition);
    }

    public PropositionDto accepter(String email, Long propositionId, String commentaire) {
        PropositionOffre proposition = chargerPourCandidat(email, propositionId);
        verifierActionnable(proposition);
        return propositionService.accepter(propositionId, new AcceptionRequest(CANDIDAT, commentaire, null));
    }

    public PropositionDto refuser(String email, Long propositionId, String commentaire) {
        PropositionOffre proposition = chargerPourCandidat(email, propositionId);
        verifierActionnable(proposition);
        return propositionService.refuser(propositionId, new ActionRequest(CANDIDAT, commentaire));
    }

    public NegociationDto revendiquer(String email, Long propositionId, RevendicationRequest request) {
        PropositionOffre proposition = chargerPourCandidat(email, propositionId);
        verifierActionnable(proposition);
        if (request.salaireRevendique() == null
                && (request.avantageRevendique() == null || request.avantageRevendique().isBlank())) {
            throw ApiException.badRequest("REVENDICATION_VIDE",
                    "Indiquez un salaire ou un avantage à négocier.");
        }
        RevendicationRequest demandeCandidat = new RevendicationRequest(CANDIDAT, TypeNegociation.REVENDICATION,
                request.salaireRevendique(), request.avantageRevendique(), request.motif());
        return negociationService.deposerRevendication(propositionId, demandeCandidat);
    }

    private Long candidatId(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.forbidden("COMPTE_CANDIDAT_INVALIDE",
                        "Le compte candidat est introuvable."));
        if (utilisateur.getCandidat() == null) {
            throw ApiException.forbidden("PROFIL_CANDIDAT_MANQUANT",
                    "Aucun profil candidat n'est associé à ce compte.");
        }
        return utilisateur.getCandidat().getId();
    }

    private PropositionOffre chargerPourCandidat(String email, Long propositionId) {
        Long candidatId = candidatId(email);
        return propositionRepository.findByIdAndCandidature_Candidat_Id(propositionId, candidatId)
                .orElseThrow(() -> ApiException.notFound("Proposition introuvable."));
    }

    private void verifierVisible(PropositionOffre proposition) {
        if (proposition.getStatut() == StatutProposition.BROUILLON
                || proposition.getStatut() == StatutProposition.VALIDEE
                || proposition.getStatut() == StatutProposition.REVISE) {
            throw ApiException.notFound("Proposition introuvable.");
        }
    }

    private void verifierActionnable(PropositionOffre proposition) {
        verifierVisible(proposition);
        if (proposition.getStatut() != StatutProposition.EMISE) {
            throw ApiException.conflit("PROPOSITION_NON_ACTIONNABLE",
                    "Cette proposition n'attend plus de réponse.");
        }
        if (proposition.getDateLimiteValidite() != null
                && proposition.getDateLimiteValidite().isBefore(LocalDate.now())) {
            throw ApiException.conflit("PROPOSITION_EXPIREE",
                    "Le délai de réponse à cette proposition est dépassé.");
        }
    }
}
