package com.mrtovo.backend.controller;

import com.mrtovo.backend.service.TempsTravailService;
import com.mrtovo.backend.service.PointageService;
import com.mrtovo.backend.dto.SemaineTempsDto;
import com.mrtovo.backend.dto.ChangementMotDePasseRequest;
import com.mrtovo.backend.dto.DemandeCongeDto;
import com.mrtovo.backend.dto.DroitCongeDto;
import com.mrtovo.backend.dto.SalarieProfilDto;
import com.mrtovo.backend.service.CompteSalarieService;
import com.mrtovo.backend.service.CongeService;
import com.mrtovo.backend.service.EspaceSalarieService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** Bloc 7 cote salarie. Le salarie est toujours deduit du compte connecte, jamais de la requete. */
@RestController
@RequestMapping("/api/salarie")
public class EspaceSalarieController {

    private final EspaceSalarieService espaceService;
    private final CongeService congeService;
    private final SoldeCongeService soldeService;
    private final CompteSalarieService compteService;
    private final PointageService pointageService;
    private final TempsTravailService tempsService;

    public EspaceSalarieController(EspaceSalarieService espaceService,
                                   CongeService congeService,
                                   SoldeCongeService soldeService,
                                   CompteSalarieService compteService,
                                   PointageService pointageService,
                                   TempsTravailService tempsService) {
        this.espaceService = espaceService;
        this.congeService = congeService;
        this.soldeService = soldeService;
        this.compteService = compteService;
        this.pointageService = pointageService;
        this.tempsService = tempsService;
    }

    // Bloc 6 : le salarie pointe lui-meme. Routes reservees au role EMPLOYE dans SecurityConfig.

    /** Sa semaine, en lecture seule : la meme que celle que voit le RH. */
    @GetMapping("/pointage")
    public SemaineTempsDto maSemaine(@RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     Authentication auth) {
        return tempsService.semaine(espaceService.employeId(auth.getName()), date != null ? date : LocalDate.now());
    }

    @PostMapping("/pointage/arrivee")
    public SemaineTempsDto pointerArrivee(Authentication auth) {
        return pointageService.pointerArrivee(espaceService.employeId(auth.getName()), auth.getName());
    }

    @PostMapping("/pointage/depart")
    public SemaineTempsDto pointerDepart(Authentication auth) {
        return pointageService.pointerDepart(espaceService.employeId(auth.getName()), auth.getName());
    }

    @GetMapping("/moi")
    public SalarieProfilDto moi(Authentication auth) {
        return espaceService.profil(auth.getName());
    }

    /** Les mouvements de solde du salarie connecte (ajustements et indemnisations du RH). */
    @GetMapping("/mouvements")
    public List<DroitCongeDto> mesMouvements(Authentication auth) {
        return soldeService.mouvements(espaceService.employeId(auth.getName()));
    }

    @GetMapping("/conges")
    public List<DemandeCongeDto> mesDemandes(Authentication auth) {
        return congeService.demandesDuSalarie(espaceService.employeId(auth.getName()));
    }

    @PostMapping(value = "/conges", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeCongeDto demander(
            @RequestParam Long typeCongeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) String motif,
            @RequestPart(required = false) MultipartFile justificatif,
            Authentication auth) {
        return congeService.demander(espaceService.employeId(auth.getName()), typeCongeId, dateDebut, dateFin,
                motif, justificatif, auth.getName());
    }

    @PostMapping(value = "/conges/{id}/justificatif", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DemandeCongeDto ajouterJustificatif(@PathVariable Long id,
                                               @RequestPart MultipartFile justificatif,
                                               Authentication auth) {
        return congeService.ajouterJustificatif(espaceService.employeId(auth.getName()), id, justificatif, auth.getName());
    }

    @GetMapping("/conges/{id}/justificatif")
    public ResponseEntity<Resource> justificatif(@PathVariable Long id, Authentication auth) {
        return DossierSalarieController.reponseFichier(
                congeService.justificatifDuSalarie(espaceService.employeId(auth.getName()), id));
    }

    @PostMapping("/conges/{id}/annuler")
    public DemandeCongeDto annuler(@PathVariable Long id, Authentication auth) {
        return congeService.annulerParSalarie(espaceService.employeId(auth.getName()), id, auth.getName());
    }

    @PutMapping("/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changerMotDePasse(@Valid @RequestBody ChangementMotDePasseRequest requete, Authentication auth) {
        compteService.changerMotDePasse(auth.getName(), requete);
    }
}
