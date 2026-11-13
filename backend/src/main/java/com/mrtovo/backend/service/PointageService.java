package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.CorrectionPointageRequest;
import com.mrtovo.backend.dto.DecisionHeuresSupRequest;
import com.mrtovo.backend.dto.PointageRequest;
import com.mrtovo.backend.dto.SemaineTempsDto;
import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.DecisionHeuresSup;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.Pointage;
import com.mrtovo.backend.entity.SourcePointage;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.StatutHeuresSup;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.DecisionHeuresSupRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.PointageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bloc 6 : tout ce qui s'ecrit sur les pointages et les heures sup. Chaque ecriture est tracee. */
@Service
@Transactional
public class PointageService {

    private static final String RAISON_POINTAGE_MODIFIE = "Un pointage de la semaine a changé.";

    private final EmployeEmbaucheRepository employeRepository;
    private final PointageRepository pointageRepository;
    private final DecisionHeuresSupRepository decisionRepository;
    private final TempsTravailService tempsService;
    private final TraceTempsService trace;
    private final Clock horloge;

    @Autowired
    public PointageService(EmployeEmbaucheRepository employeRepository,
                           PointageRepository pointageRepository,
                           DecisionHeuresSupRepository decisionRepository,
                           TempsTravailService tempsService,
                           TraceTempsService trace) {
        this(employeRepository, pointageRepository, decisionRepository, tempsService, trace,
                Clock.systemDefaultZone());
    }

    PointageService(EmployeEmbaucheRepository employeRepository,
                    PointageRepository pointageRepository,
                    DecisionHeuresSupRepository decisionRepository,
                    TempsTravailService tempsService,
                    TraceTempsService trace,
                    Clock horloge) {
        this.employeRepository = employeRepository;
        this.pointageRepository = pointageRepository;
        this.decisionRepository = decisionRepository;
        this.tempsService = tempsService;
        this.trace = trace;
        this.horloge = horloge;
    }

    /** Ajout par le RH d'un pointage oublie : le salarie pointe d'ordinaire lui-meme, d'ou le motif obligatoire. */
    public SemaineTempsDto saisir(PointageRequest requete, String auteur) {
        String motif = nettoyer(requete.motif());
        if (motif == null) {
            throw ApiException.badRequest("MOTIF_OBLIGATOIRE",
                    "Le salarié pointe lui-même : indiquez pourquoi vous ajoutez ce pointage.");
        }
        EmployeEmbauche employe = employeRepository.findById(requete.employeId())
                .orElseThrow(() -> ApiException.notFound("Salarié introuvable : " + requete.employeId()));
        verifierDate(employe, requete.date());
        verifierHeures(requete.heureEntree(), requete.heureSortie());
        if (pointageRepository.findByEmployeIdAndDatePointage(employe.getId(), requete.date()).isPresent()) {
            throw ApiException.conflit("POINTAGE_EXISTANT",
                    "Un pointage existe déjà pour ce salarié le " + requete.date() + " : corrigez-le.");
        }

        Pointage pointage = new Pointage();
        pointage.setEmploye(employe);
        pointage.setDatePointage(requete.date());
        pointage.setHeureEntree(requete.heureEntree());
        pointage.setHeureSortie(requete.heureSortie());
        pointage.setCommentaire(nettoyer(requete.commentaire()));
        pointage.setSaisiPar(auteur);
        pointage.setSource(SourcePointage.RH);
        pointageRepository.save(pointage);

        trace.tracer(employe, ActionAudit.SAISIE_POINTAGE, auteur, decrire(pointage) + " — motif : " + motif);
        rouvrirSemaine(employe, requete.date(), auteur);
        return tempsService.semaine(employe.getId(), requete.date());
    }

    public SemaineTempsDto corriger(Long pointageId, CorrectionPointageRequest requete, String auteur) {
        Pointage pointage = chargerPointage(pointageId);
        EmployeEmbauche employe = pointage.getEmploye();
        verifierHeures(requete.heureEntree(), requete.heureSortie());

        List<String> changements = new ArrayList<>();
        comparer(changements, "Entrée", pointage.getHeureEntree(), requete.heureEntree());
        comparer(changements, "Sortie", pointage.getHeureSortie(), requete.heureSortie());
        comparer(changements, "Commentaire", pointage.getCommentaire(), nettoyer(requete.commentaire()));
        if (changements.isEmpty()) {
            return tempsService.semaine(employe.getId(), pointage.getDatePointage());
        }

        pointage.setHeureEntree(requete.heureEntree());
        pointage.setHeureSortie(requete.heureSortie());
        pointage.setCommentaire(nettoyer(requete.commentaire()));
        pointage.setModifiePar(auteur);
        pointage.setDateModification(LocalDateTime.now(horloge));
        pointage.setSource(SourcePointage.CORRIGE_RH);
        pointageRepository.save(pointage);

        trace.tracer(employe, ActionAudit.CORRECTION_POINTAGE, auteur,
                pointage.getDatePointage() + " — motif : " + requete.motif().trim() + "\n"
                        + String.join("\n", changements));
        rouvrirSemaine(employe, pointage.getDatePointage(), auteur);
        return tempsService.semaine(employe.getId(), pointage.getDatePointage());
    }

    // =====================================================================
    // Pointage par le salarie lui-meme
    // =====================================================================

    /** L'heure est celle du serveur, a la minute : le salarie ne la choisit pas. */
    public SemaineTempsDto pointerArrivee(Long employeId, String auteur) {
        EmployeEmbauche employe = employeRepository.findById(employeId)
                .orElseThrow(() -> ApiException.notFound("Salarié introuvable : " + employeId));
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        LocalDate aujourdhui = maintenant.toLocalDate();
        verifierDate(employe, aujourdhui);
        if (pointageRepository.findByEmployeIdAndDatePointage(employeId, aujourdhui).isPresent()) {
            throw ApiException.conflit("ARRIVEE_DEJA_POINTEE", "Votre arrivée est déjà pointée aujourd'hui.");
        }

        Pointage pointage = new Pointage();
        pointage.setEmploye(employe);
        pointage.setDatePointage(aujourdhui);
        pointage.setHeureEntree(maintenant.toLocalTime().truncatedTo(ChronoUnit.MINUTES));
        pointage.setSaisiPar(auteur);
        pointage.setSource(SourcePointage.SALARIE);
        pointageRepository.save(pointage);

        trace.tracer(employe, ActionAudit.POINTAGE_ARRIVEE, auteur, decrire(pointage));
        return tempsService.semaine(employeId, aujourdhui);
    }

    public SemaineTempsDto pointerDepart(Long employeId, String auteur) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        LocalDate aujourdhui = maintenant.toLocalDate();
        Pointage pointage = pointageRepository.findByEmployeIdAndDatePointage(employeId, aujourdhui)
                .orElseThrow(() -> ApiException.conflit("ARRIVEE_NON_POINTEE",
                        "Pointez d'abord votre arrivée."));
        if (pointage.getHeureSortie() != null) {
            throw ApiException.conflit("DEPART_DEJA_POINTE", "Votre départ est déjà pointé aujourd'hui.");
        }
        LocalTime sortie = maintenant.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        if (!sortie.isAfter(pointage.getHeureEntree())) {
            throw ApiException.conflit("DEPART_TROP_TOT",
                    "Le départ doit suivre l'arrivée (" + pointage.getHeureEntree() + ").");
        }
        pointage.setHeureSortie(sortie);
        pointageRepository.save(pointage);

        trace.tracer(pointage.getEmploye(), ActionAudit.POINTAGE_DEPART, auteur, decrire(pointage));
        return tempsService.semaine(employeId, aujourdhui);
    }

    public SemaineTempsDto supprimer(Long pointageId, String motif, String auteur) {
        if (nettoyer(motif) == null) {
            throw ApiException.badRequest("MOTIF_OBLIGATOIRE", "Le motif de suppression est obligatoire.");
        }
        Pointage pointage = chargerPointage(pointageId);
        EmployeEmbauche employe = pointage.getEmploye();
        LocalDate date = pointage.getDatePointage();

        trace.tracer(employe, ActionAudit.SUPPRESSION_POINTAGE, auteur, decrire(pointage) + " — motif : " + motif.trim());
        pointageRepository.delete(pointage);
        pointageRepository.flush();
        rouvrirSemaine(employe, date, auteur);
        return tempsService.semaine(employe.getId(), date);
    }

    public SemaineTempsDto deciderHeuresSup(DecisionHeuresSupRequest requete, String auteur) {
        LocalDate lundi = requete.semaineDebut();
        if (lundi.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw ApiException.badRequest("SEMAINE_INVALIDE", "La semaine doit être désignée par son lundi.");
        }
        if (requete.decision() != StatutHeuresSup.VALIDEE && requete.decision() != StatutHeuresSup.REFUSEE) {
            throw ApiException.badRequest("DECISION_INVALIDE", "La décision doit être VALIDEE ou REFUSEE.");
        }

        SemaineTempsDto semaine = tempsService.semaine(requete.employeId(), lundi);
        SemaineTempsDto.Decision etat = semaine.decision();
        if (!etat.validable()) {
            throw ApiException.conflit("HEURES_SUP_NON_DECIDABLES", etat.raisonBlocage());
        }

        boolean validee = requete.decision() == StatutHeuresSup.VALIDEE;
        int minutes = validee ? requete.minutesValidees() : 0;
        String commentaire = nettoyer(requete.commentaire());
        if (validee && (minutes < 1 || minutes > etat.minutesBrutes())) {
            throw ApiException.badRequest("MINUTES_INVALIDES",
                    "Les minutes validées doivent être comprises entre 1 et " + etat.minutesBrutes() + ".");
        }
        if ((!validee || minutes < etat.minutesBrutes()) && commentaire == null) {
            throw ApiException.badRequest("COMMENTAIRE_OBLIGATOIRE",
                    "Un refus ou une validation partielle doit être justifié.");
        }

        DecisionHeuresSup decision = decisionRepository
                .findByEmployeIdAndSemaineDebut(requete.employeId(), lundi)
                .orElseGet(DecisionHeuresSup::new);
        boolean nouvelle = decision.getId() == null;
        String avant = nouvelle ? "" : " (remplace " + decision.getStatut() + " : " + decision.getMinutesValidees() + " min)";

        EmployeEmbauche employe = employeRepository.getReferenceById(requete.employeId());
        decision.setEmploye(employe);
        decision.setSemaineDebut(lundi);
        decision.setStatut(requete.decision());
        decision.setMinutesBrutes(etat.minutesBrutes());
        decision.setMinutesValidees(minutes);
        decision.setCommentaire(commentaire);
        decision.setDecidePar(auteur);
        decision.setDateDecision(LocalDateTime.now(horloge));
        decisionRepository.save(decision);

        trace.tracer(employe, ActionAudit.DECISION_HEURES_SUP, auteur,
                "Semaine du " + lundi + " : " + requete.decision() + ", " + minutes + " min sur "
                        + etat.minutesBrutes() + avant + (commentaire != null ? " — " + commentaire : ""));
        return tempsService.semaine(requete.employeId(), lundi);
    }

    /** Le RH saisit a posteriori : une date passee est normale, une date future ne peut pas etre un fait. */
    private void verifierDate(EmployeEmbauche employe, LocalDate date) {
        if (date.isAfter(LocalDate.now(horloge))) {
            throw ApiException.badRequest("DATE_FUTURE", "On ne peut pas pointer une date future.");
        }
        if (date.isBefore(employe.getDateEntree())) {
            throw ApiException.badRequest("DATE_AVANT_ENTREE",
                    "Le salarié n'était pas encore entré le " + date + ".");
        }
        if (employe.getStatut() == StatutEmploye.SORTI) {
            throw ApiException.conflit("SALARIE_SORTI", "Ce salarié a quitté les effectifs.");
        }
    }

    private static void verifierHeures(LocalTime entree, LocalTime sortie) {
        if (sortie != null && !sortie.isAfter(entree)) {
            throw ApiException.badRequest("HEURES_INCOHERENTES", "L'heure de sortie doit suivre l'heure d'entrée.");
        }
    }

    private void rouvrirSemaine(EmployeEmbauche employe, LocalDate date, String auteur) {
        trace.rouvrirDecision(employe, TempsTravailService.lundiDe(date), RAISON_POINTAGE_MODIFIE, auteur);
    }

    private Pointage chargerPointage(Long pointageId) {
        return pointageRepository.findById(pointageId)
                .orElseThrow(() -> ApiException.notFound("Pointage introuvable : " + pointageId));
    }

    private static void comparer(List<String> changements, String champ, Object avant, Object apres) {
        if (!Objects.equals(avant, apres)) {
            changements.add(champ + " : " + (avant != null ? avant : "—") + " → " + (apres != null ? apres : "—"));
        }
    }

    private static String decrire(Pointage pointage) {
        return pointage.getDatePointage() + " : " + pointage.getHeureEntree() + " → "
                + (pointage.getHeureSortie() != null ? pointage.getHeureSortie() : "sortie non pointée");
    }

    private static String nettoyer(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
