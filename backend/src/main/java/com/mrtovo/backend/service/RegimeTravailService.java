package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.AffectationRegimeRequest;
import com.mrtovo.backend.dto.PlanningSalarieDto;
import com.mrtovo.backend.dto.RegimeTravailDto;
import com.mrtovo.backend.dto.RegimeTravailRequest;
import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.HoraireRegime;
import com.mrtovo.backend.entity.PlanningSalarie;
import com.mrtovo.backend.entity.RegimeTravail;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.DecisionHeuresSupRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.PlanningSalarieRepository;
import com.mrtovo.backend.repository.RegimeTravailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Bloc 6 : definition des regimes de travail et planning de chaque salarie. */
@Service
@Transactional
public class RegimeTravailService {

    private final RegimeTravailRepository regimeRepository;
    private final PlanningSalarieRepository planningRepository;
    private final EmployeEmbaucheRepository employeRepository;
    private final DecisionHeuresSupRepository decisionRepository;
    private final TraceTempsService trace;

    public RegimeTravailService(RegimeTravailRepository regimeRepository,
                                PlanningSalarieRepository planningRepository,
                                EmployeEmbaucheRepository employeRepository,
                                DecisionHeuresSupRepository decisionRepository,
                                TraceTempsService trace) {
        this.regimeRepository = regimeRepository;
        this.planningRepository = planningRepository;
        this.employeRepository = employeRepository;
        this.decisionRepository = decisionRepository;
        this.trace = trace;
    }

    @Transactional(readOnly = true)
    public List<RegimeTravailDto> lister() {
        return regimeRepository.findAllByOrderByLibelleAsc().stream().map(RegimeTravailService::toDto).toList();
    }

    public RegimeTravailDto creer(RegimeTravailRequest requete, String auteur) {
        String code = requete.code().trim().toUpperCase(Locale.ROOT);
        if (regimeRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw ApiException.conflit("REGIME_EXISTANT", "Le code " + code + " est déjà utilisé.");
        }

        RegimeTravail regime = new RegimeTravail();
        regime.setCode(code);
        regime.setLibelle(requete.libelle().trim());
        regime.setToleranceRetardMinutes(requete.toleranceRetardMinutes());
        regime.setCreePar(auteur);

        Set<Integer> jours = new HashSet<>();
        for (RegimeTravailRequest.Horaire horaire : requete.horaires()) {
            if (!jours.add(horaire.jourSemaine())) {
                throw ApiException.badRequest("JOUR_EN_DOUBLE", "Un même jour apparaît deux fois dans la grille.");
            }
            if (!horaire.heureFin().isAfter(horaire.heureDebut())) {
                throw ApiException.badRequest("HORAIRE_INVALIDE", "La fin doit suivre le début.");
            }
            long amplitude = Duration.between(horaire.heureDebut(), horaire.heureFin()).toMinutes();
            if (horaire.pauseMinutes() >= amplitude) {
                throw ApiException.badRequest("PAUSE_INVALIDE", "La pause doit être plus courte que la journée.");
            }
            HoraireRegime ligne = new HoraireRegime();
            ligne.setJourSemaine(horaire.jourSemaine());
            ligne.setHeureDebut(horaire.heureDebut());
            ligne.setHeureFin(horaire.heureFin());
            ligne.setPauseMinutes(horaire.pauseMinutes());
            regime.ajouterHoraire(ligne);
        }
        return toDto(regimeRepository.save(regime));
    }

    @Transactional(readOnly = true)
    public List<PlanningSalarieDto> plannings(Long employeId) {
        chargerEmploye(employeId);
        return planningRepository.findByEmployeIdOrderByDateDebutAsc(employeId).stream()
                .map(RegimeTravailService::toDto)
                .toList();
    }

    /**
     * Ouvre une nouvelle periode et ferme la precedente : l'historique des regimes reste consultable.
     * Si la date tombe dans le passe, les semaines deja decidees sont recalculees et donc rouvertes.
     */
    public PlanningSalarieDto affecter(Long employeId, AffectationRegimeRequest requete, String auteur) {
        EmployeEmbauche employe = chargerEmploye(employeId);
        RegimeTravail regime = regimeRepository.findById(requete.regimeId())
                .orElseThrow(() -> ApiException.notFound("Régime introuvable : " + requete.regimeId()));
        LocalDate debut = requete.dateDebut();
        if (debut.isBefore(employe.getDateEntree())) {
            throw ApiException.badRequest("DATE_AVANT_ENTREE",
                    "Le régime ne peut pas commencer avant l'entrée du salarié (" + employe.getDateEntree() + ").");
        }

        List<PlanningSalarie> existants = planningRepository.findByEmployeIdOrderByDateDebutAsc(employeId);
        if (existants.stream().anyMatch(planning -> !planning.getDateDebut().isBefore(debut))) {
            throw ApiException.conflit("PLANNING_POSTERIEUR",
                    "Une période commence déjà à cette date ou plus tard.");
        }
        existants.stream()
                .filter(planning -> planning.getDateFin() == null || !planning.getDateFin().isBefore(debut))
                .forEach(planning -> planning.setDateFin(debut.minusDays(1)));

        PlanningSalarie planning = new PlanningSalarie();
        planning.setEmploye(employe);
        planning.setRegime(regime);
        planning.setDateDebut(debut);
        planning.setAffectePar(auteur);
        planningRepository.save(planning);

        trace.tracer(employe, ActionAudit.AFFECTATION_REGIME, auteur,
                regime.getLibelle() + " à partir du " + debut);
        decisionRepository.findByEmployeIdAndSemaineDebutGreaterThanEqual(employeId, TempsTravailService.lundiDe(debut))
                .forEach(decision -> trace.rouvrir(decision, "Le régime de travail a changé.", auteur));
        return toDto(planning);
    }

    private EmployeEmbauche chargerEmploye(Long employeId) {
        return employeRepository.findById(employeId)
                .orElseThrow(() -> ApiException.notFound("Salarié introuvable : " + employeId));
    }

    private static RegimeTravailDto toDto(RegimeTravail regime) {
        return new RegimeTravailDto(
                regime.getId(),
                regime.getCode(),
                regime.getLibelle(),
                regime.getToleranceRetardMinutes(),
                regime.minutesHebdomadaires(),
                regime.getHoraires().stream()
                        .map(horaire -> new RegimeTravailDto.Horaire(horaire.getJourSemaine(),
                                horaire.getHeureDebut(), horaire.getHeureFin(), horaire.getPauseMinutes()))
                        .toList());
    }

    private static PlanningSalarieDto toDto(PlanningSalarie planning) {
        return new PlanningSalarieDto(
                planning.getId(),
                planning.getRegime().getId(),
                planning.getRegime().getLibelle(),
                planning.getDateDebut(),
                planning.getDateFin(),
                planning.getAffectePar());
    }
}
