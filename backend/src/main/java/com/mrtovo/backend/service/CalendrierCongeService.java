package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.CalendrierCongeDto;
import com.mrtovo.backend.dto.JourTempsDto;
import com.mrtovo.backend.entity.DemandeConge;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.NatureCaseCalendrier;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.StatutAbsence;
import com.mrtovo.backend.entity.StatutDemandeConge;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.DemandeCongeRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bloc 7 : calendrier des conges. Rien n'est stocke : le mois est recompose a chaque lecture
 * a partir des demandes et des jours calcules par le Bloc 6.
 */
@Service
@Transactional(readOnly = true)
public class CalendrierCongeService {

    private final UtilisateurRepository utilisateurRepository;
    private final DemandeCongeRepository demandeRepository;
    private final TempsTravailService tempsService;

    public CalendrierCongeService(UtilisateurRepository utilisateurRepository,
                                  DemandeCongeRepository demandeRepository,
                                  TempsTravailService tempsService) {
        this.utilisateurRepository = utilisateurRepository;
        this.demandeRepository = demandeRepository;
        this.tempsService = tempsService;
    }

    /** Le filtre departement ne vaut que pour le RH : un manager reste toujours sur le sien. */
    public CalendrierCongeDto calendrier(YearMonth mois, String departement, String auteur) {
        Utilisateur lecteur = utilisateurRepository.findByEmailIgnoreCase(auteur)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable : " + auteur));
        boolean rh = lecteur.getRole() == RoleUtilisateur.RH;
        LocalDate du = mois.atDay(1);
        LocalDate au = mois.atEndOfMonth();

        List<TempsTravailService.JoursSalarie> visibles = tempsService.joursDeLEffectif(du, au).stream()
                .filter(salarie -> rh || CongeService.memeDepartement(lecteur, salarie.employe()))
                .toList();
        List<String> departements = visibles.stream()
                .map(salarie -> nomDepartement(salarie.employe()))
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<TempsTravailService.JoursSalarie> affiches = visibles.stream()
                .filter(salarie -> !rh || departement == null || departement.isBlank()
                        || departement.equalsIgnoreCase(nomDepartement(salarie.employe())))
                .sorted(Comparator.comparing((TempsTravailService.JoursSalarie salarie) -> salarie.employe().getNomComplet()))
                .toList();

        Map<Long, List<DemandeConge>> demandes = new HashMap<>();
        for (DemandeConge demande : demandeRepository.surPeriode(du, au, StatutDemandeConge.ACTIFS)) {
            demandes.computeIfAbsent(demande.getEmploye().getId(), id -> new ArrayList<>()).add(demande);
        }

        Compteurs compteurs = new Compteurs(mois);
        List<CalendrierCongeDto.Ligne> lignes = new ArrayList<>();
        for (TempsTravailService.JoursSalarie salarie : affiches) {
            lignes.add(ligne(salarie, demandes.getOrDefault(salarie.employe().getId(), List.of()), rh, compteurs));
        }
        return new CalendrierCongeDto(mois.toString(), du, au, rh, departements, lignes, compteurs.synthese());
    }

    private static CalendrierCongeDto.Ligne ligne(TempsTravailService.JoursSalarie salarie, List<DemandeConge> demandes,
                                                  boolean rh, Compteurs compteurs) {
        EmployeEmbauche employe = salarie.employe();
        List<CalendrierCongeDto.Case> cases = new ArrayList<>();
        for (JourTempsDto jour : salarie.jours()) {
            // Deux demandes actives ne se chevauchent jamais : un jour porte au plus une demande.
            DemandeConge demande = demandes.stream()
                    .filter(candidate -> candidate.couvre(jour.date()))
                    .findFirst()
                    .orElse(null);
            compteurs.compter(jour, demande);

            CalendrierCongeDto.Case detail = caseDuJour(jour, demande);
            if (detail != null) {
                cases.add(rh ? detail : masquer(detail));
            }
        }
        return new CalendrierCongeDto.Ligne(employe.getId(), employe.getMatricule(), employe.getNomComplet(),
                nomDepartement(employe), cases);
    }

    /** La demande passe avant l'absence : une maladie declaree apres coup explique le jour manquant. */
    private static CalendrierCongeDto.Case caseDuJour(JourTempsDto jour, DemandeConge demande) {
        if (demande != null) {
            return new CalendrierCongeDto.Case(jour.date(),
                    demande.getStatut().enAttente() ? NatureCaseCalendrier.EN_ATTENTE : NatureCaseCalendrier.CONGE,
                    demande.getTypeConge().getLibelle(),
                    demande.getId(),
                    demande.getTypeConge().getId(),
                    demande.getStatut(),
                    demande.getDateDebut(),
                    demande.getDateFin());
        }
        if (!jour.absent()) {
            return null;
        }
        NatureCaseCalendrier nature = natureAbsence(jour.absenceStatut());
        String libelle = switch (nature) {
            case ABSENCE_JUSTIFIEE -> "Absence justifiée";
            case ABSENCE_NON_JUSTIFIEE -> "Absence non justifiée";
            default -> "Absence à qualifier";
        };
        return new CalendrierCongeDto.Case(jour.date(), nature, libelle, null, null, null, jour.date(), jour.date());
    }

    private static NatureCaseCalendrier natureAbsence(StatutAbsence statut) {
        if (statut == null) {
            return NatureCaseCalendrier.ABSENCE_A_QUALIFIER;
        }
        return statut == StatutAbsence.JUSTIFIEE
                ? NatureCaseCalendrier.ABSENCE_JUSTIFIEE
                : NatureCaseCalendrier.ABSENCE_NON_JUSTIFIEE;
    }

    /** Point unique du masquage : ce que le manager n'a pas a voir ne quitte jamais le serveur. */
    private static CalendrierCongeDto.Case masquer(CalendrierCongeDto.Case detail) {
        boolean enAttente = detail.nature() == NatureCaseCalendrier.EN_ATTENTE;
        return new CalendrierCongeDto.Case(detail.date(),
                enAttente ? NatureCaseCalendrier.EN_ATTENTE : NatureCaseCalendrier.ABSENT,
                enAttente ? "Absence en attente" : "Absent",
                null, null, null, null, null);
    }

    private static String nomDepartement(EmployeEmbauche employe) {
        return employe.getDepartement() != null ? employe.getDepartement().getNom() : null;
    }

    /** Totaux par jour de la population affichee, salaries sans regime exclus : on ne sait pas s'ils sont attendus. */
    private static final class Compteurs {

        private final YearMonth mois;
        private final int[] attendus;
        private final int[] presents;
        private final int[] enAttente;

        private Compteurs(YearMonth mois) {
            int jours = mois.lengthOfMonth();
            this.mois = mois;
            this.attendus = new int[jours];
            this.presents = new int[jours];
            this.enAttente = new int[jours];
        }

        private void compter(JourTempsDto jour, DemandeConge demande) {
            if (jour.heureDebutPrevue() == null) {
                return;
            }
            int indice = jour.date().getDayOfMonth() - 1;
            boolean congeValide = demande != null && demande.getStatut() == StatutDemandeConge.VALIDEE;
            attendus[indice]++;
            if (congeValide || jour.absent()) {
                return;
            }
            presents[indice]++;
            if (demande != null) {
                enAttente[indice]++;
            }
        }

        private List<CalendrierCongeDto.Synthese> synthese() {
            List<CalendrierCongeDto.Synthese> jours = new ArrayList<>();
            for (int i = 0; i < attendus.length; i++) {
                jours.add(new CalendrierCongeDto.Synthese(mois.atDay(i + 1), attendus[i], presents[i], enAttente[i]));
            }
            return jours;
        }
    }
}
