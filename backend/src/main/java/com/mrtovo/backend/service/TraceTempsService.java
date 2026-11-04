package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.DecisionHeuresSup;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.JournalAudit;
import com.mrtovo.backend.repository.DecisionHeuresSupRepository;
import com.mrtovo.backend.repository.JournalAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Point commun aux services du Bloc 6 : ecrit dans le journal d'audit du Bloc 5
 * et annule une decision d'heures sup quand les faits de la semaine changent.
 */
@Service
@Transactional
public class TraceTempsService {

    private final JournalAuditRepository journalRepository;
    private final DecisionHeuresSupRepository decisionRepository;

    public TraceTempsService(JournalAuditRepository journalRepository,
                             DecisionHeuresSupRepository decisionRepository) {
        this.journalRepository = journalRepository;
        this.decisionRepository = decisionRepository;
    }

    public void tracer(EmployeEmbauche employe, ActionAudit action, String auteur, String details) {
        journalRepository.save(new JournalAudit(employe, action, auteur != null ? auteur : "inconnu", details));
    }

    /** Sans effet s'il n'y a pas de decision pour cette semaine. */
    public void rouvrirDecision(EmployeEmbauche employe, LocalDate lundi, String raison, String auteur) {
        decisionRepository.findByEmployeIdAndSemaineDebut(employe.getId(), lundi)
                .ifPresent(decision -> rouvrir(decision, raison, auteur));
    }

    public void rouvrir(DecisionHeuresSup decision, String raison, String auteur) {
        tracer(decision.getEmploye(), ActionAudit.REOUVERTURE_HEURES_SUP, auteur,
                "Semaine du " + decision.getSemaineDebut() + " : décision " + decision.getStatut()
                        + " (" + decision.getMinutesValidees() + " min validées) annulée. " + raison);
        decisionRepository.delete(decision);
        // Force le DELETE tout de suite : une nouvelle decision peut suivre dans la meme transaction.
        decisionRepository.flush();
    }
}
