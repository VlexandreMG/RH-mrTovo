package com.mrtovo.backend.bloc3.service;

import com.mrtovo.backend.bloc3.domain.Affectation;
import com.mrtovo.backend.bloc3.domain.Candidat;
import com.mrtovo.backend.bloc3.domain.Contrat;
import com.mrtovo.backend.bloc3.domain.Employe;
import com.mrtovo.backend.bloc3.domain.PropositionOffre;
import com.mrtovo.backend.bloc3.domain.StatutAffectation;
import com.mrtovo.backend.bloc3.domain.StatutContrat;
import com.mrtovo.backend.bloc3.domain.StatutEmploye;
import com.mrtovo.backend.bloc3.domain.TypeAffectation;
import com.mrtovo.backend.bloc3.domain.TypeContrat;
import com.mrtovo.backend.bloc3.exception.ApiException;
import com.mrtovo.backend.bloc3.repository.AffectationRepository;
import com.mrtovo.backend.bloc3.repository.ContratRepository;
import com.mrtovo.backend.bloc3.repository.EmployeRepository;
import com.mrtovo.backend.bloc3.web.dto.ActionRequest;
import com.mrtovo.backend.bloc3.web.dto.ContratDto;
import com.mrtovo.backend.bloc3.web.dto.EmployeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final ContratRepository contratRepository;
    private final AffectationRepository affectationRepository;
    private final IdentifiantService identifiantService;
    private final Bloc3Mapper mapper;

    public EmployeService(EmployeRepository employeRepository,
                          ContratRepository contratRepository,
                          AffectationRepository affectationRepository,
                          IdentifiantService identifiantService,
                          Bloc3Mapper mapper) {
        this.employeRepository = employeRepository;
        this.contratRepository = contratRepository;
        this.affectationRepository = affectationRepository;
        this.identifiantService = identifiantService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<EmployeDto> lister() {
        return employeRepository.findAll().stream().map(mapper::toEmployeDto).toList();
    }

    @Transactional(readOnly = true)
    public EmployeDto detail(Long id) {
        return mapper.toEmployeDto(employeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Employe introuvable : " + id)));
    }

    @Transactional(readOnly = true)
    public List<ContratDto> listerContrats() {
        return contratRepository.findAllByOrderByDateDebutDesc().stream().map(mapper::toContratDto).toList();
    }

    @Transactional(readOnly = true)
    public ContratDto detailContrat(Long id) {
        return mapper.toContratDto(contratRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Contrat introuvable : " + id)));
    }

    public EmployeDto embaucher(PropositionOffre proposition, LocalDate dateEmbauche) {
        if (employeRepository.findByPropositionId(proposition.getId()).isPresent()) {
            throw ApiException.conflit("EMPLOYE_EXISTANT",
                    "Un employe existe deja pour la proposition " + proposition.getId());
        }
        if (proposition.getPoste() == null) {
            throw ApiException.badRequest("POSTE_MANQUANT", "La proposition ne comporte aucun poste : affectation impossible");
        }

        Candidat candidat = proposition.getCandidature().getCandidat();
        int periodeEssai = proposition.getPeriodeEssaiMois() == null ? 0 : proposition.getPeriodeEssaiMois();
        LocalDate dateEntree = proposition.getDatePrisePoste();
        LocalDate finPeriodeEssai = periodeEssai > 0 ? dateEntree.plusMonths(periodeEssai) : null;

        Employe employe = new Employe();
        employe.setMatricule(identifiantService.prochainMatricule());
        employe.setCandidat(candidat);
        employe.setProposition(proposition);
        employe.setNom(candidat.getNom());
        employe.setPrenom(candidat.getPrenom());
        employe.setEmail(candidat.getEmail());
        employe.setTelephone(candidat.getTelephone());
        employe.setPoste(proposition.getPoste());
        employe.setDepartement(proposition.getDepartement());
        employe.setDateEntree(dateEntree);
        employe.setDateEmbauche(dateEmbauche != null ? dateEmbauche : LocalDate.now());
        employe.setDateFinPeriodeEssai(finPeriodeEssai);
        employe.setDateFinContrat(proposition.getDateFinContrat());
        employe.setStatut(periodeEssai > 0 ? StatutEmploye.EN_PERIODE_DESSAI : StatutEmploye.ACTIF);
        employeRepository.save(employe);

        Contrat contrat = new Contrat();
        contrat.setEmploye(employe);
        contrat.setReference(identifiantService.prochaineReferenceContrat());
        TypeContrat typeInitial = periodeEssai > 0 && proposition.getTypeContrat() == TypeContrat.CDI
                ? TypeContrat.PERIODE_DESSAI
                : proposition.getTypeContrat();
        contrat.setTypeContrat(typeInitial);
        contrat.setDateDebut(dateEntree);
        contrat.setDateFin(typeInitial == TypeContrat.PERIODE_DESSAI ? finPeriodeEssai : proposition.getDateFinContrat());
        contrat.setSalaire(proposition.getSalaireBase());
        contrat.setStatutContrat(StatutContrat.A_SIGNER);
        contratRepository.save(contrat);

        Affectation affectation = new Affectation();
        affectation.setEmploye(employe);
        affectation.setPoste(proposition.getPoste());
        affectation.setDepartement(proposition.getDepartement());
        affectation.setType(TypeAffectation.INITIALE);
        affectation.setDateDebut(dateEntree);
        affectation.setStatut(StatutAffectation.ACTIVE);
        affectation.setMotif("Embauche : " + proposition.getPoste().getTitre());
        affectationRepository.save(affectation);

        return mapper.toEmployeDto(employe);
    }

    public ContratDto signerContrat(Long contratId, ActionRequest request) {
        Contrat contrat = contratRepository.findById(contratId)
                .orElseThrow(() -> ApiException.notFound("Contrat introuvable : " + contratId));
        if (contrat.getStatutContrat() != StatutContrat.A_SIGNER) {
            throw ApiException.conflit("CONTRAT_NON_SIGNABLE",
                    "Le contrat " + contrat.getReference() + " n'est pas en attente de signature ("
                            + contrat.getStatutContrat() + ")");
        }
        contrat.setStatutContrat(StatutContrat.SIGNE);
        contratRepository.save(contrat);
        return mapper.toContratDto(contrat);
    }

    public ContratDto activerContrat(Long contratId, ActionRequest request) {
        Contrat contrat = contratRepository.findById(contratId)
                .orElseThrow(() -> ApiException.notFound("Contrat introuvable : " + contratId));
        if (contrat.getStatutContrat() != StatutContrat.SIGNE
                && contrat.getStatutContrat() != StatutContrat.EN_PERIODE_DESSAI
                && contrat.getStatutContrat() != StatutContrat.A_SIGNER) {
            throw ApiException.conflit("CONTRAT_NON_ACTIVABLE",
                    "Le contrat " + contrat.getReference() + " ne peut pas etre active (" + contrat.getStatutContrat() + ")");
        }
        contrat.setStatutContrat(StatutContrat.ACTIF);
        contratRepository.save(contrat);

        Employe employe = contrat.getEmploye();
        if (employe != null && employe.getStatut() == StatutEmploye.EN_PERIODE_DESSAI) {
            employe.setStatut(StatutEmploye.ACTIF);
            employeRepository.save(employe);
        }
        return mapper.toContratDto(contrat);
    }
}
