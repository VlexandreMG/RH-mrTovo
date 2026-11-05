package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.AbsenceDto;
import com.mrtovo.backend.dto.JourTempsDto;
import com.mrtovo.backend.entity.Absence;
import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.StatutAbsence;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.AbsenceRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bloc 7 : le Bloc 6 repere les jours absents, le RH leur donne un sens. */
@Service
@Transactional
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final EmployeEmbaucheRepository employeRepository;
    private final TempsTravailService tempsService;
    private final DocumentStorageService stockage;
    private final TraceTempsService trace;

    public AbsenceService(AbsenceRepository absenceRepository,
                          EmployeEmbaucheRepository employeRepository,
                          TempsTravailService tempsService,
                          DocumentStorageService stockage,
                          TraceTempsService trace) {
        this.absenceRepository = absenceRepository;
        this.employeRepository = employeRepository;
        this.tempsService = tempsService;
        this.stockage = stockage;
        this.trace = trace;
    }

    /** Jours a qualifier d'abord, puis jours deja qualifies, du plus recent au plus ancien. */
    @Transactional(readOnly = true)
    public List<AbsenceDto> lister(LocalDate du, LocalDate au) {
        List<AbsenceDto> resultat = new ArrayList<>();
        for (TempsTravailService.JourAQualifier jour : tempsService.absencesAQualifier(du, au)) {
            EmployeEmbauche employe = jour.employe();
            resultat.add(new AbsenceDto(null, employe.getId(), employe.getMatricule(), employe.getNomComplet(),
                    jour.date(), null, null, false, null, null));
        }
        absenceRepository.findByDateAbsenceBetween(du, au).stream()
                .sorted(Comparator.comparing(Absence::getDateAbsence).reversed())
                .map(AbsenceService::toDto)
                .forEach(resultat::add);
        return resultat;
    }

    public AbsenceDto qualifier(Long employeId, LocalDate date, StatutAbsence statut, String motif,
                                MultipartFile justificatif, String auteur) {
        EmployeEmbauche employe = employeRepository.findById(employeId)
                .orElseThrow(() -> ApiException.notFound("Salarié introuvable : " + employeId));
        JourTempsDto jour = tempsService.semaine(employeId, date).jours().stream()
                .filter(candidat -> candidat.date().equals(date))
                .findFirst()
                .orElseThrow();
        if (!jour.absent()) {
            throw ApiException.conflit("PAS_ABSENT", "Le salarié n'est pas absent à cette date.");
        }

        Absence absence = absenceRepository.findByEmployeIdAndDateAbsence(employeId, date).orElseGet(Absence::new);
        String motifNettoye = motif == null || motif.isBlank() ? null : motif.trim();
        boolean fichierFourni = justificatif != null && !justificatif.isEmpty();
        if (statut == StatutAbsence.JUSTIFIEE
                && (motifNettoye == null || (!fichierFourni && absence.getJustificatifCle() == null))) {
            throw ApiException.badRequest("JUSTIFICATIF_MANQUANT",
                    "Une absence justifiée demande un motif et un justificatif.");
        }

        String avant = absence.getStatut() != null ? " (remplace " + absence.getStatut().getLibelle() + ")" : "";
        absence.setEmploye(employe);
        absence.setDateAbsence(date);
        absence.setStatut(statut);
        absence.setMotif(motifNettoye);
        absence.setQualifiePar(auteur);
        absence.setDateQualification(LocalDateTime.now());
        if (fichierFourni) {
            String ancienneCle = absence.getJustificatifCle();
            DocumentStorageService.FichierStocke stocke = stockage.enregistrer(justificatif);
            absence.setJustificatifCle(stocke.cle());
            absence.setJustificatifType(stocke.contentType());
            absence.setJustificatifNom(DossierSalarieService.nomFichierSur(justificatif.getOriginalFilename()));
            DossierSalarieService.apresAnnulation(() -> stockage.supprimer(stocke.cle()));
            if (ancienneCle != null) {
                DossierSalarieService.apresValidation(() -> stockage.supprimer(ancienneCle));
            }
        }
        absenceRepository.save(absence);

        trace.tracer(employe, ActionAudit.QUALIFICATION_ABSENCE, auteur,
                date + " : " + statut.getLibelle() + avant + (motifNettoye != null ? " — " + motifNettoye : ""));
        return toDto(absence);
    }

    public DossierSalarieService.FichierTelecharge justificatif(Long absenceId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> ApiException.notFound("Absence introuvable : " + absenceId));
        if (absence.getJustificatifCle() == null) {
            throw ApiException.notFound("Aucun justificatif sur cette absence.");
        }
        return new DossierSalarieService.FichierTelecharge(stockage.charger(absence.getJustificatifCle()),
                absence.getJustificatifType(), absence.getJustificatifNom());
    }

    private static AbsenceDto toDto(Absence absence) {
        EmployeEmbauche employe = absence.getEmploye();
        return new AbsenceDto(absence.getId(), employe.getId(), employe.getMatricule(), employe.getNomComplet(),
                absence.getDateAbsence(), absence.getStatut(), absence.getMotif(),
                absence.getJustificatifCle() != null, absence.getJustificatifNom(), absence.getQualifiePar());
    }
}
