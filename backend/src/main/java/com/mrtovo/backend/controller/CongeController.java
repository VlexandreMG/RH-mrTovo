package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.AbsenceDto;
import com.mrtovo.backend.dto.AjustementDroitRequest;
import com.mrtovo.backend.dto.AnnulationCongeRequest;
import com.mrtovo.backend.dto.CalendrierCongeDto;
import com.mrtovo.backend.dto.CompteSalarieRequest;
import com.mrtovo.backend.dto.DecisionCongeRequest;
import com.mrtovo.backend.dto.DemandeCongeDto;
import com.mrtovo.backend.dto.DroitCongeDto;
import com.mrtovo.backend.dto.IndemnisationRequest;
import com.mrtovo.backend.dto.SoldeCongeDto;
import com.mrtovo.backend.dto.TypeCongeDto;
import com.mrtovo.backend.dto.TypeCongeRequest;
import com.mrtovo.backend.entity.StatutAbsence;
import com.mrtovo.backend.service.AbsenceService;
import com.mrtovo.backend.service.CalendrierCongeService;
import com.mrtovo.backend.service.CompteSalarieService;
import com.mrtovo.backend.service.CongeService;
import com.mrtovo.backend.service.SoldeCongeService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Bloc 7 cote gestionnaires.
 * /api/bloc7/** : RH et managers ; /api/bloc7/rh/** : RH uniquement (regles dans SecurityConfig).
 */
@RestController
@RequestMapping("/api/bloc7")
public class CongeController {

    private final CongeService congeService;
    private final SoldeCongeService soldeService;
    private final AbsenceService absenceService;
    private final CompteSalarieService compteService;
    private final CalendrierCongeService calendrierService;

    public CongeController(CongeService congeService,
                           SoldeCongeService soldeService,
                           AbsenceService absenceService,
                           CompteSalarieService compteService,
                           CalendrierCongeService calendrierService) {
        this.congeService = congeService;
        this.soldeService = soldeService;
        this.absenceService = absenceService;
        this.compteService = compteService;
        this.calendrierService = calendrierService;
    }

    @GetMapping("/demandes")
    public List<DemandeCongeDto> demandes(Authentication auth) {
        return congeService.demandesPour(auth.getName());
    }

    /** Ouvert au manager : le service limite a son departement et retire ce qu'il n'a pas a voir. */
    @GetMapping("/calendrier")
    public CalendrierCongeDto calendrier(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois,
                                         @RequestParam(required = false) String departement,
                                         Authentication auth) {
        return calendrierService.calendrier(mois, departement, auth.getName());
    }

    @PostMapping("/demandes/{id}/avis")
    public DemandeCongeDto avisManager(@PathVariable Long id, @Valid @RequestBody DecisionCongeRequest requete,
                                       Authentication auth) {
        return congeService.avisManager(id, requete, auth.getName());
    }

    @GetMapping("/demandes/{id}/justificatif")
    public ResponseEntity<Resource> justificatif(@PathVariable Long id, Authentication auth) {
        return DossierSalarieController.reponseFichier(congeService.justificatifPour(id, auth.getName()));
    }

    @PostMapping("/rh/demandes/{id}/decision")
    public DemandeCongeDto decisionRh(@PathVariable Long id, @Valid @RequestBody DecisionCongeRequest requete,
                                      Authentication auth) {
        return congeService.decisionRh(id, requete, auth.getName());
    }

    @PostMapping("/rh/demandes/{id}/annuler")
    public DemandeCongeDto annuler(@PathVariable Long id, @Valid @RequestBody AnnulationCongeRequest requete,
                                   Authentication auth) {
        return congeService.annulerParRh(id, requete.commentaire(), auth.getName());
    }

    @GetMapping("/rh/types")
    public List<TypeCongeDto> types() {
        return congeService.types();
    }

    @PostMapping("/rh/types")
    @ResponseStatus(HttpStatus.CREATED)
    public TypeCongeDto creerType(@Valid @RequestBody TypeCongeRequest requete) {
        return congeService.creerType(requete);
    }

    @GetMapping("/rh/soldes")
    public List<SoldeCongeDto> soldes() {
        return soldeService.soldes();
    }

    @GetMapping("/rh/employes/{id}/mouvements")
    public List<DroitCongeDto> mouvements(@PathVariable Long id) {
        return soldeService.mouvements(id);
    }

    @PostMapping("/rh/employes/{id}/ajustement")
    public SoldeCongeDto ajuster(@PathVariable Long id, @Valid @RequestBody AjustementDroitRequest requete,
                                 Authentication auth) {
        return soldeService.ajuster(id, requete, auth.getName());
    }

    @PostMapping("/rh/employes/{id}/indemnisation")
    public SoldeCongeDto indemniser(@PathVariable Long id, @Valid @RequestBody IndemnisationRequest requete,
                                    Authentication auth) {
        return soldeService.indemniser(id, requete, auth.getName());
    }

    /** 204 et non 201 : le client HTTP du projet lit un corps JSON pour tout statut sauf 204. */
    @PostMapping("/rh/employes/{id}/compte")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void creerCompte(@PathVariable Long id, @Valid @RequestBody CompteSalarieRequest requete,
                            Authentication auth) {
        compteService.creerCompte(id, requete.motDePasse(), auth.getName());
    }

    @GetMapping("/rh/absences")
    public List<AbsenceDto> absences(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return absenceService.lister(du, au);
    }

    @PostMapping(value = "/rh/absences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AbsenceDto qualifier(@RequestParam Long employeId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestParam StatutAbsence statut,
                                @RequestParam(required = false) String motif,
                                @RequestPart(required = false) MultipartFile justificatif,
                                Authentication auth) {
        return absenceService.qualifier(employeId, date, statut, motif, justificatif, auth.getName());
    }

    @GetMapping("/rh/absences/{id}/justificatif")
    public ResponseEntity<Resource> justificatifAbsence(@PathVariable Long id) {
        return DossierSalarieController.reponseFichier(absenceService.justificatif(id));
    }
}
