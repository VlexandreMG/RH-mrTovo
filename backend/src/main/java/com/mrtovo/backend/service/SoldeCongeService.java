package com.mrtovo.backend.service;

import com.mrtovo.backend.config.CongeProperties;
import com.mrtovo.backend.dto.AjustementDroitRequest;
import com.mrtovo.backend.dto.DroitCongeDto;
import com.mrtovo.backend.dto.IndemnisationRequest;
import com.mrtovo.backend.dto.SoldeCongeDto;
import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.Contrat;
import com.mrtovo.backend.entity.DemandeConge;
import com.mrtovo.backend.entity.DroitConge;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.StatutDemandeConge;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.TypeDroitConge;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.ContratRepository;
import com.mrtovo.backend.repository.DemandeCongeRepository;
import com.mrtovo.backend.repository.DroitCongeRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bloc 7 : droits et soldes. Le solde n'est jamais stocke :
 * solde = droits acquis + mouvements du RH - jours valides.
 */
@Service
@Transactional(readOnly = true)
public class SoldeCongeService {

    private final EmployeEmbaucheRepository employeRepository;
    private final DemandeCongeRepository demandeRepository;
    private final DroitCongeRepository droitRepository;
    private final ContratRepository contratRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TraceTempsService trace;
    private final CongeProperties proprietes;
    private final Clock horloge;

    @Autowired
    public SoldeCongeService(EmployeEmbaucheRepository employeRepository,
                             DemandeCongeRepository demandeRepository,
                             DroitCongeRepository droitRepository,
                             ContratRepository contratRepository,
                             UtilisateurRepository utilisateurRepository,
                             TraceTempsService trace,
                             CongeProperties proprietes) {
        this(employeRepository, demandeRepository, droitRepository, contratRepository, utilisateurRepository,
                trace, proprietes, Clock.systemDefaultZone());
    }

    SoldeCongeService(EmployeEmbaucheRepository employeRepository,
                      DemandeCongeRepository demandeRepository,
                      DroitCongeRepository droitRepository,
                      ContratRepository contratRepository,
                      UtilisateurRepository utilisateurRepository,
                      TraceTempsService trace,
                      CongeProperties proprietes,
                      Clock horloge) {
        this.employeRepository = employeRepository;
        this.demandeRepository = demandeRepository;
        this.droitRepository = droitRepository;
        this.contratRepository = contratRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.trace = trace;
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    public SoldeCongeDto solde(Long employeId) {
        EmployeEmbauche employe = chargerEmploye(employeId);
        boolean compte = utilisateurRepository.findByEmployeId(employeId).isPresent();
        return calculer(employe, compte);
    }

    public List<SoldeCongeDto> soldes() {
        Set<Long> avecCompte = utilisateurRepository.findByEmployeIsNotNull().stream()
                .map(Utilisateur::getEmploye)
                .map(EmployeEmbauche::getId)
                .collect(Collectors.toSet());
        return employeRepository.findAll().stream()
                .filter(employe -> employe.getStatut() != StatutEmploye.SORTI)
                // Les dossiers minimaux des comptes non salaries n'ont pas de solde a gerer.
                .filter(employe -> !employe.isDossierMinimal())
                .sorted(Comparator.comparing(EmployeEmbauche::getMatricule))
                .map(employe -> calculer(employe, avecCompte.contains(employe.getId())))
                .toList();
    }

    public List<DroitCongeDto> mouvements(Long employeId) {
        chargerEmploye(employeId);
        return droitRepository.findByEmployeIdOrderByDateCreationDesc(employeId).stream()
                .map(droit -> new DroitCongeDto(droit.getId(), droit.getTypeDroit(), droit.getJours(), droit.getMotif(),
                        droit.getMoisPaie(), droit.getMontant(), droit.getCreePar(), droit.getDateCreation()))
                .toList();
    }

    @Transactional
    public SoldeCongeDto ajuster(Long employeId, AjustementDroitRequest requete, String auteur) {
        if (requete.jours().signum() == 0) {
            throw ApiException.badRequest("JOURS_INVALIDES", "L'ajustement ne peut pas être nul.");
        }
        EmployeEmbauche employe = verrouiller(employeId);
        if (requete.jours().signum() < 0 && requete.jours().negate().compareTo(calculer(employe, false).disponible()) > 0) {
            throw ApiException.conflit("SOLDE_INSUFFISANT", "Le solde disponible ne permet pas ce retrait.");
        }
        DroitConge droit = nouveauDroit(employe, TypeDroitConge.AJUSTEMENT, requete.jours(), requete.motif(), auteur);
        droitRepository.save(droit);
        trace.tracer(employe, ActionAudit.AJUSTEMENT_DROIT_CONGE, auteur,
                requete.jours() + " jour(s) — " + requete.motif().trim());
        return solde(employeId);
    }

    /** Paie des jours non pris : conge = salaire brut x jours non pris / 30. Le montant est fige. */
    @Transactional
    public SoldeCongeDto indemniser(Long employeId, IndemnisationRequest requete, String auteur) {
        EmployeEmbauche employe = verrouiller(employeId);
        SoldeCongeDto solde = calculer(employe, false);
        if (requete.jours().compareTo(solde.disponible()) > 0) {
            throw ApiException.conflit("SOLDE_INSUFFISANT",
                    "Seuls " + solde.disponible() + " jour(s) sont disponibles.");
        }
        if (solde.salaireBrut() == null) {
            throw ApiException.conflit("CONTRAT_MANQUANT", "Aucun contrat en cours pour calculer l'indemnité.");
        }
        BigDecimal montant = CalculConge.montantJours(solde.salaireBrut(), requete.jours());
        DroitConge droit = nouveauDroit(employe, TypeDroitConge.INDEMNISATION, requete.jours().negate(),
                requete.motif(), auteur);
        droit.setMoisPaie(requete.moisPaie());
        droit.setMontant(montant);
        droitRepository.save(droit);
        trace.tracer(employe, ActionAudit.INDEMNISATION_CONGE, auteur,
                requete.jours() + " jour(s) non pris = " + montant + " Ar, paie de " + requete.moisPaie()
                        + " — " + requete.motif().trim());
        return solde(employeId);
    }

    private SoldeCongeDto calculer(EmployeEmbauche employe, boolean compteActif) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        int mois = CalculConge.moisComplets(employe.getDateEntree(), aujourdhui);
        BigDecimal acquis = CalculConge.joursAcquis(mois, proprietes.getJoursAcquisParMois());

        List<DroitConge> droits = droitRepository.findByEmployeIdOrderByDateCreationDesc(employe.getId());
        BigDecimal ajustes = sommeDroits(droits, TypeDroitConge.AJUSTEMENT);
        BigDecimal indemnises = sommeDroits(droits, TypeDroitConge.INDEMNISATION).negate();

        List<DemandeConge> demandes = demandeRepository.findByEmployeIdOrderByDateDebutDesc(employe.getId()).stream()
                .filter(demande -> demande.getTypeConge().isDecompteSolde())
                .toList();
        BigDecimal pris = sommeJours(demandes.stream().filter(d -> d.getStatut() == StatutDemandeConge.VALIDEE).toList());
        BigDecimal attente = sommeJours(demandes.stream().filter(d -> d.getStatut().enAttente()).toList());

        BigDecimal solde = acquis.add(ajustes).subtract(indemnises).subtract(pris);
        BigDecimal disponible = solde.subtract(attente);

        Contrat contrat = DossierSalarieService.contratEnCours(
                contratRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId()), aujourdhui);
        BigDecimal salaire = contrat != null ? contrat.getSalaire() : null;
        BigDecimal indemnite = salaire != null
                ? CalculConge.montantJours(salaire, solde.max(BigDecimal.ZERO))
                : null;

        return new SoldeCongeDto(employe.getId(), employe.getMatricule(), employe.getNomComplet(), mois,
                acquis, ajustes, pris, indemnises, solde, attente, disponible, salaire, indemnite, compteActif);
    }

    private static BigDecimal sommeDroits(List<DroitConge> droits, TypeDroitConge type) {
        return droits.stream()
                .filter(droit -> droit.getTypeDroit() == type)
                .map(DroitConge::getJours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sommeJours(List<DemandeConge> demandes) {
        return BigDecimal.valueOf(demandes.stream().mapToInt(DemandeConge::getNbJours).sum());
    }

    private static DroitConge nouveauDroit(EmployeEmbauche employe, TypeDroitConge type, BigDecimal jours,
                                           String motif, String auteur) {
        DroitConge droit = new DroitConge();
        droit.setEmploye(employe);
        droit.setTypeDroit(type);
        droit.setJours(jours);
        droit.setMotif(motif.trim());
        droit.setCreePar(auteur);
        return droit;
    }

    private EmployeEmbauche verrouiller(Long employeId) {
        return employeRepository.verrouiller(employeId)
                .orElseThrow(() -> ApiException.notFound("Salarié introuvable : " + employeId));
    }

    private EmployeEmbauche chargerEmploye(Long employeId) {
        return employeRepository.findById(employeId)
                .orElseThrow(() -> ApiException.notFound("Salarié introuvable : " + employeId));
    }
}
