package com.mrtovo.backend.service;

import com.mrtovo.backend.config.TempsTravailProperties;
import com.mrtovo.backend.dto.AnomaliePointageDto;
import com.mrtovo.backend.dto.JourTempsDto;
import com.mrtovo.backend.dto.SemaineTempsDto;
import com.mrtovo.backend.dto.VariablesPaieDto;
import com.mrtovo.backend.entity.Contrat;
import com.mrtovo.backend.entity.DecisionHeuresSup;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.HoraireRegime;
import com.mrtovo.backend.entity.PlanningSalarie;
import com.mrtovo.backend.entity.Pointage;
import com.mrtovo.backend.entity.RegimeTravail;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.StatutHeuresSup;
import com.mrtovo.backend.entity.TypeAnomaliePointage;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.ContratRepository;
import com.mrtovo.backend.repository.DecisionHeuresSupRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.PlanningSalarieRepository;
import com.mrtovo.backend.repository.PointageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bloc 6 : tout ce qui se lit. Les retards, heures sup et anomalies ne sont jamais stockes :
 * ils sont recalcules ici depuis les pointages et le planning, donc toujours coherents avec eux.
 */
@Service
@Transactional(readOnly = true)
public class TempsTravailService {

    /** Profondeur de recherche de la liste « heures sup a valider ». */
    static final int SEMAINES_A_VALIDER_MAX = 12;
    /** Evite qu'une recherche d'anomalies recalcule plusieurs annees d'un coup. */
    static final int PERIODE_ANOMALIES_MAX_JOURS = 92;

    private final EmployeEmbaucheRepository employeRepository;
    private final PointageRepository pointageRepository;
    private final PlanningSalarieRepository planningRepository;
    private final DecisionHeuresSupRepository decisionRepository;
    private final ContratRepository contratRepository;
    private final TempsTravailProperties reglages;
    private final Clock horloge;

    @Autowired
    public TempsTravailService(EmployeEmbaucheRepository employeRepository,
                               PointageRepository pointageRepository,
                               PlanningSalarieRepository planningRepository,
                               DecisionHeuresSupRepository decisionRepository,
                               ContratRepository contratRepository,
                               TempsTravailProperties reglages) {
        this(employeRepository, pointageRepository, planningRepository, decisionRepository,
                contratRepository, reglages, Clock.systemDefaultZone());
    }

    TempsTravailService(EmployeEmbaucheRepository employeRepository,
                        PointageRepository pointageRepository,
                        PlanningSalarieRepository planningRepository,
                        DecisionHeuresSupRepository decisionRepository,
                        ContratRepository contratRepository,
                        TempsTravailProperties reglages,
                        Clock horloge) {
        this.employeRepository = employeRepository;
        this.pointageRepository = pointageRepository;
        this.planningRepository = planningRepository;
        this.decisionRepository = decisionRepository;
        this.contratRepository = contratRepository;
        this.reglages = reglages;
        this.horloge = horloge;
    }

    // =====================================================================
    // Lectures exposees par l'API
    // =====================================================================

    public SemaineTempsDto semaine(Long employeId, LocalDate jour) {
        EmployeEmbauche employe = employeRepository.findById(employeId)
                .orElseThrow(() -> ApiException.notFound("Salarié introuvable : " + employeId));
        LocalDate lundi = lundiDe(jour);
        return calculerSemaine(employe, lundi, chargerDonnees(lundi, lundi.plusDays(6)), LocalDate.now(horloge));
    }

    public List<SemaineTempsDto> heuresSupAValider() {
        LocalDate aujourdhui = LocalDate.now(horloge);
        LocalDate derniereSemaine = lundiDe(aujourdhui).minusWeeks(1);
        LocalDate premiereSemaine = derniereSemaine.minusWeeks(SEMAINES_A_VALIDER_MAX - 1L);
        Donnees donnees = chargerDonnees(premiereSemaine, derniereSemaine.plusDays(6));

        List<SemaineTempsDto> resultat = new ArrayList<>();
        for (EmployeEmbauche employe : effectif()) {
            for (LocalDate lundi = premiereSemaine; !lundi.isAfter(derniereSemaine); lundi = lundi.plusWeeks(1)) {
                SemaineTempsDto semaine = calculerSemaine(employe, lundi, donnees, aujourdhui);
                if (semaine.decision().statut() == StatutHeuresSup.A_VALIDER) {
                    resultat.add(semaine);
                }
            }
        }
        resultat.sort(Comparator.comparing(SemaineTempsDto::semaineDebut).thenComparing(SemaineTempsDto::matricule));
        return resultat;
    }

    public List<AnomaliePointageDto> anomalies(LocalDate du, LocalDate au) {
        if (au.isBefore(du)) {
            throw ApiException.badRequest("PERIODE_INVALIDE", "La date de fin précède la date de début.");
        }
        if (ChronoUnit.DAYS.between(du, au) > PERIODE_ANOMALIES_MAX_JOURS) {
            throw ApiException.badRequest("PERIODE_TROP_LONGUE",
                    "La période ne peut pas dépasser " + PERIODE_ANOMALIES_MAX_JOURS + " jours.");
        }
        LocalDate aujourdhui = LocalDate.now(horloge);
        LocalDate premierLundi = lundiDe(du);
        LocalDate dernierLundi = lundiDe(au);
        Donnees donnees = chargerDonnees(premierLundi, dernierLundi.plusDays(6));

        List<AnomaliePointageDto> resultat = new ArrayList<>();
        for (EmployeEmbauche employe : effectif()) {
            if (donnees.planningsDe(employe).isEmpty()) {
                resultat.add(anomalie(employe, au, TypeAnomaliePointage.SANS_REGIME,
                        "Affecter un régime pour calculer ses horaires"));
            }
            for (LocalDate lundi = premierLundi; !lundi.isAfter(dernierLundi); lundi = lundi.plusWeeks(1)) {
                for (JourTempsDto jour : calculerSemaine(employe, lundi, donnees, aujourdhui).jours()) {
                    if (!jour.date().isBefore(du) && !jour.date().isAfter(au)) {
                        resultat.addAll(jour.anomalies());
                    }
                }
            }
        }
        resultat.sort(Comparator.comparing(AnomaliePointageDto::date).reversed()
                .thenComparing(AnomaliePointageDto::matricule));
        return resultat;
    }

    /**
     * Variables transmises a la paie. Une semaine appartient au mois de son lundi :
     * elle n'est ainsi jamais coupee en deux, ce qui garde la compensation hebdomadaire exacte.
     */
    public List<VariablesPaieDto> variablesPaie(YearMonth mois) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        List<LocalDate> lundis = lundisDu(mois);
        LocalDate dernierJour = lundis.get(lundis.size() - 1).plusDays(6);
        Donnees donnees = chargerDonnees(lundis.get(0), dernierJour);

        return effectif().stream()
                .filter(employe -> !employe.getDateEntree().isAfter(dernierJour))
                .map(employe -> variablesDe(employe, lundis, dernierJour, donnees, aujourdhui))
                .toList();
    }

    // =====================================================================
    // Calcul d'une semaine
    // =====================================================================

    private SemaineTempsDto calculerSemaine(EmployeEmbauche employe, LocalDate lundi, Donnees donnees,
                                            LocalDate aujourdhui) {
        LocalDate dimanche = lundi.plusDays(6);
        List<JourTempsDto> jours = new ArrayList<>();
        String regimeLibelle = null;

        for (LocalDate date = lundi; !date.isAfter(dimanche); date = date.plusDays(1)) {
            PlanningSalarie planning = planningDu(employe, date, donnees);
            if (planning != null) {
                regimeLibelle = planning.getRegime().getLibelle();
            }
            jours.add(calculerJour(employe, date, planning, donnees.pointage(employe, date), aujourdhui));
        }

        int prevues = jours.stream().mapToInt(JourTempsDto::minutesPrevues).sum();
        int travaillees = jours.stream().mapToInt(JourTempsDto::minutesTravaillees).sum();
        int retard = jours.stream().mapToInt(JourTempsDto::minutesRetard).sum();
        int depart = jours.stream().mapToInt(JourTempsDto::minutesDepartAnticipe).sum();
        int hsBrutes = jours.stream().mapToInt(JourTempsDto::minutesHeuresSup).sum();
        int absents = (int) jours.stream().filter(JourTempsDto::absent).count();
        int anomalies = jours.stream().mapToInt(jour -> jour.anomalies().size()).sum();

        DecisionHeuresSup existante = donnees.decision(employe, lundi);
        SemaineTempsDto.Decision decision = construireDecision(existante, hsBrutes, jours, dimanche, aujourdhui);
        CalculTemps.Compensation bilan = CalculTemps.compenser(
                retard + depart, decision.minutesValidees(), reglages.getHsPalier1Minutes());

        return new SemaineTempsDto(
                employe.getId(), employe.getMatricule(), employe.getNomComplet(), regimeLibelle,
                lundi, dimanche, jours, prevues, travaillees, retard, depart, absents, anomalies,
                decision,
                new SemaineTempsDto.Bilan(bilan.minutesDues(), bilan.compensees(), bilan.minutesRetenues(),
                        bilan.hsPayees(), bilan.hsPalier1(), bilan.hsPalier2()));
    }

    private JourTempsDto calculerJour(EmployeEmbauche employe, LocalDate date, PlanningSalarie planning,
                                      Pointage pointage, LocalDate aujourdhui) {
        boolean dansEffectif = !date.isBefore(employe.getDateEntree());
        RegimeTravail regime = planning != null ? planning.getRegime() : null;
        HoraireRegime prevu = regime != null && dansEffectif ? regime.horaireDu(date.getDayOfWeek().getValue()) : null;

        CalculTemps.HoraireJour horaire = prevu != null
                ? new CalculTemps.HoraireJour(prevu.getHeureDebut(), prevu.getHeureFin(), prevu.getPauseMinutes())
                : null;
        int tolerance = regime != null ? regime.getToleranceRetardMinutes() : 0;
        // Sans regime on ne sait pas ce qui etait prevu : on ne deduit donc ni retard ni heures sup.
        CalculTemps.ResultatJour resultat = regime == null
                ? new CalculTemps.ResultatJour(0, 0, 0, 0, 0)
                : CalculTemps.calculerJour(horaire,
                        pointage != null ? pointage.getHeureEntree() : null,
                        pointage != null ? pointage.getHeureSortie() : null,
                        tolerance);

        boolean absent = horaire != null && pointage == null && date.isBefore(aujourdhui);
        List<AnomaliePointageDto> anomalies = detecterAnomalies(
                employe, date, regime, horaire, pointage, resultat, absent, aujourdhui);

        return new JourTempsDto(
                date,
                horaire != null ? horaire.debut() : null,
                horaire != null ? horaire.fin() : null,
                pointage != null ? pointage.getId() : null,
                pointage != null ? pointage.getHeureEntree() : null,
                pointage != null ? pointage.getHeureSortie() : null,
                pointage != null ? pointage.getCommentaire() : null,
                absent,
                resultat.prevues(),
                resultat.travaillees(),
                resultat.retard(),
                resultat.departAnticipe(),
                resultat.heuresSup(),
                anomalies);
    }

    private List<AnomaliePointageDto> detecterAnomalies(EmployeEmbauche employe, LocalDate date, RegimeTravail regime,
                                                        CalculTemps.HoraireJour horaire, Pointage pointage,
                                                        CalculTemps.ResultatJour resultat, boolean absent,
                                                        LocalDate aujourdhui) {
        List<AnomaliePointageDto> anomalies = new ArrayList<>();
        if (absent) {
            anomalies.add(anomalie(employe, date, TypeAnomaliePointage.POINTAGE_MANQUANT,
                    "Attendu de " + horaire.debut() + " à " + horaire.fin()));
        }
        if (pointage == null) {
            return anomalies;
        }
        if (regime == null) {
            anomalies.add(anomalie(employe, date, TypeAnomaliePointage.SANS_REGIME,
                    "Pointage impossible à évaluer sans régime"));
            return anomalies;
        }
        if (pointage.getHeureSortie() == null && date.isBefore(aujourdhui)) {
            anomalies.add(anomalie(employe, date, TypeAnomaliePointage.SORTIE_MANQUANTE,
                    "Entrée à " + pointage.getHeureEntree()));
        }
        if (horaire == null) {
            anomalies.add(anomalie(employe, date, TypeAnomaliePointage.TRAVAIL_JOUR_REPOS,
                    "Entrée à " + pointage.getHeureEntree()));
        }
        if (resultat.retard() > reglages.getSeuilRetardExcessifMinutes()) {
            anomalies.add(anomalie(employe, date, TypeAnomaliePointage.RETARD_EXCESSIF,
                    resultat.retard() + " min de retard"));
        }
        if (resultat.departAnticipe() > 0) {
            anomalies.add(anomalie(employe, date, TypeAnomaliePointage.DEPART_ANTICIPE,
                    resultat.departAnticipe() + " min avant la fin"));
        }
        if (resultat.travaillees() > reglages.getSeuilPresenceExcessiveMinutes()) {
            anomalies.add(anomalie(employe, date, TypeAnomaliePointage.PRESENCE_EXCESSIVE,
                    resultat.travaillees() + " min travaillées"));
        }
        return anomalies;
    }

    private SemaineTempsDto.Decision construireDecision(DecisionHeuresSup existante, int hsBrutes,
                                                        List<JourTempsDto> jours, LocalDate dimanche,
                                                        LocalDate aujourdhui) {
        String blocage = raisonBlocage(hsBrutes, jours, dimanche, aujourdhui);
        if (existante != null) {
            return new SemaineTempsDto.Decision(existante.getStatut(), hsBrutes, existante.getMinutesValidees(),
                    blocage == null, blocage, existante.getCommentaire(), existante.getDecidePar());
        }
        StatutHeuresSup statut = hsBrutes > 0 ? StatutHeuresSup.A_VALIDER : StatutHeuresSup.SANS_HS;
        return new SemaineTempsDto.Decision(statut, hsBrutes, 0, blocage == null, blocage, null, null);
    }

    /** Une decision sur une semaine incomplete porterait sur des chiffres qui peuvent encore bouger. */
    private static String raisonBlocage(int hsBrutes, List<JourTempsDto> jours, LocalDate dimanche, LocalDate aujourdhui) {
        if (hsBrutes == 0) {
            return "Aucune heure supplémentaire sur cette semaine.";
        }
        if (!dimanche.isBefore(aujourdhui)) {
            return "La semaine n'est pas terminée.";
        }
        boolean sortieManquante = jours.stream()
                .anyMatch(jour -> jour.pointageId() != null && jour.heureSortie() == null);
        return sortieManquante ? "Une sortie reste à pointer." : null;
    }

    // =====================================================================
    // Variables de paie
    // =====================================================================

    private VariablesPaieDto variablesDe(EmployeEmbauche employe, List<LocalDate> lundis, LocalDate dernierJour,
                                         Donnees donnees, LocalDate aujourdhui) {
        int absents = 0;
        int prevues = 0;
        int travaillees = 0;
        int dues = 0;
        int compensees = 0;
        int retenues = 0;
        int palier1 = 0;
        int palier2 = 0;
        int aValider = 0;
        int anomalies = 0;
        boolean provisoire = !dernierJour.isBefore(aujourdhui);

        for (LocalDate lundi : lundis) {
            SemaineTempsDto semaine = calculerSemaine(employe, lundi, donnees, aujourdhui);
            absents += semaine.joursAbsents();
            prevues += semaine.minutesPrevues();
            travaillees += semaine.minutesTravaillees();
            dues += semaine.bilan().minutesDues();
            compensees += semaine.bilan().minutesCompensees();
            retenues += semaine.bilan().minutesRetenues();
            palier1 += semaine.bilan().hsPalier1();
            palier2 += semaine.bilan().hsPalier2();
            anomalies += semaine.nbAnomalies();
            if (semaine.decision().statut() == StatutHeuresSup.A_VALIDER) {
                aValider++;
                provisoire = true;
            }
            provisoire |= semaine.jours().stream().anyMatch(jour -> jour.pointageId() != null
                    && jour.heureSortie() == null);
        }

        Contrat contrat = DossierSalarieService.contratEnCours(
                contratRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId()), dernierJour);
        PlanningSalarie planning = planningDu(employe, dernierJour, donnees);
        BigDecimal salaire = contrat != null ? contrat.getSalaire() : null;
        BigDecimal tauxExact = planning != null && salaire != null
                ? tauxHoraire(salaire, planning.getRegime().minutesHebdomadaires())
                : null;

        return new VariablesPaieDto(
                employe.getId(), employe.getMatricule(), employe.getNomComplet(),
                absents, prevues, travaillees, dues, compensees, retenues, palier1, palier2,
                reglages.getTauxHsPalier1(), reglages.getTauxHsPalier2(), aValider, anomalies, provisoire,
                salaire,
                tauxExact != null ? tauxExact.setScale(2, RoundingMode.HALF_UP) : null,
                tauxExact != null ? montantHeuresSup(tauxExact, palier1, palier2) : null,
                tauxExact != null ? montant(tauxExact, retenues) : null);
    }

    /** Salaire mensuel / heures mensuelles, avec heures mensuelles = heures hebdo x 52 / 12 (173,33 h pour 40 h). */
    static BigDecimal tauxHoraire(BigDecimal salaire, int minutesHebdomadaires) {
        if (minutesHebdomadaires == 0) {
            return null;
        }
        return salaire.multiply(BigDecimal.valueOf(12L * 60))
                .divide(BigDecimal.valueOf(52L * minutesHebdomadaires), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal montantHeuresSup(BigDecimal taux, int palier1, int palier2) {
        BigDecimal minutesPonderees = BigDecimal.valueOf((long) palier1 * reglages.getTauxHsPalier1()
                + (long) palier2 * reglages.getTauxHsPalier2());
        return taux.multiply(minutesPonderees)
                .divide(BigDecimal.valueOf(60L * 100), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal montant(BigDecimal taux, int minutes) {
        return taux.multiply(BigDecimal.valueOf(minutes)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    // =====================================================================
    // Outils
    // =====================================================================

    public static LocalDate lundiDe(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    static List<LocalDate> lundisDu(YearMonth mois) {
        List<LocalDate> lundis = new ArrayList<>();
        LocalDate lundi = mois.atDay(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        while (YearMonth.from(lundi).equals(mois)) {
            lundis.add(lundi);
            lundi = lundi.plusWeeks(1);
        }
        return lundis;
    }

    private List<EmployeEmbauche> effectif() {
        return employeRepository.findAll().stream()
                .filter(employe -> employe.getStatut() != StatutEmploye.SORTI)
                .sorted(Comparator.comparing(EmployeEmbauche::getMatricule))
                .toList();
    }

    private static PlanningSalarie planningDu(EmployeEmbauche employe, LocalDate date, Donnees donnees) {
        return donnees.planningsDe(employe).stream()
                .filter(planning -> planning.couvre(date))
                .findFirst()
                .orElse(null);
    }

    private static AnomaliePointageDto anomalie(EmployeEmbauche employe, LocalDate date,
                                                TypeAnomaliePointage type, String detail) {
        return new AnomaliePointageDto(employe.getId(), employe.getMatricule(), employe.getNomComplet(),
                date, type, type.getLibelle(), detail);
    }

    private Donnees chargerDonnees(LocalDate du, LocalDate au) {
        Map<Long, Map<LocalDate, Pointage>> pointages = new HashMap<>();
        for (Pointage pointage : pointageRepository.findByDatePointageBetween(du, au)) {
            pointages.computeIfAbsent(pointage.getEmploye().getId(), id -> new HashMap<>())
                    .put(pointage.getDatePointage(), pointage);
        }
        Map<Long, Map<LocalDate, DecisionHeuresSup>> decisions = new HashMap<>();
        for (DecisionHeuresSup decision : decisionRepository.findBySemaineDebutBetween(du, au)) {
            decisions.computeIfAbsent(decision.getEmploye().getId(), id -> new HashMap<>())
                    .put(decision.getSemaineDebut(), decision);
        }
        Map<Long, List<PlanningSalarie>> plannings = planningRepository.chevauchant(du, au).stream()
                .collect(Collectors.groupingBy(planning -> planning.getEmploye().getId()));
        return new Donnees(pointages, plannings, decisions);
    }

    /** Tout ce qu'il faut pour une periode, charge une seule fois puis lu en memoire. */
    private record Donnees(Map<Long, Map<LocalDate, Pointage>> pointages,
                           Map<Long, List<PlanningSalarie>> plannings,
                           Map<Long, Map<LocalDate, DecisionHeuresSup>> decisions) {

        Pointage pointage(EmployeEmbauche employe, LocalDate date) {
            return pointages.getOrDefault(employe.getId(), Map.of()).get(date);
        }

        List<PlanningSalarie> planningsDe(EmployeEmbauche employe) {
            return plannings.getOrDefault(employe.getId(), List.of());
        }

        DecisionHeuresSup decision(EmployeEmbauche employe, LocalDate lundi) {
            return decisions.getOrDefault(employe.getId(), Map.of()).get(lundi);
        }
    }
}
