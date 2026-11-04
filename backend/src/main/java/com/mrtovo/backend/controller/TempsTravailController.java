package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.AffectationRegimeRequest;
import com.mrtovo.backend.dto.AnomaliePointageDto;
import com.mrtovo.backend.dto.CorrectionPointageRequest;
import com.mrtovo.backend.dto.DecisionHeuresSupRequest;
import com.mrtovo.backend.dto.PlanningSalarieDto;
import com.mrtovo.backend.dto.PointageRequest;
import com.mrtovo.backend.dto.RegimeTravailDto;
import com.mrtovo.backend.dto.RegimeTravailRequest;
import com.mrtovo.backend.dto.SemaineTempsDto;
import com.mrtovo.backend.dto.VariablesPaieDto;
import com.mrtovo.backend.service.PointageService;
import com.mrtovo.backend.service.RegimeTravailService;
import com.mrtovo.backend.service.TempsTravailService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Bloc 6 : API du temps de travail, reservee au role RH dans SecurityConfig (regle /api/bloc6/**). */
@RestController
@RequestMapping("/api/bloc6")
public class TempsTravailController {

    private final TempsTravailService tempsService;
    private final PointageService pointageService;
    private final RegimeTravailService regimeService;

    public TempsTravailController(TempsTravailService tempsService,
                                  PointageService pointageService,
                                  RegimeTravailService regimeService) {
        this.tempsService = tempsService;
        this.pointageService = pointageService;
        this.regimeService = regimeService;
    }

    @GetMapping("/regimes")
    public List<RegimeTravailDto> regimes() {
        return regimeService.lister();
    }

    @PostMapping("/regimes")
    @ResponseStatus(HttpStatus.CREATED)
    public RegimeTravailDto creerRegime(@Valid @RequestBody RegimeTravailRequest requete, Authentication auth) {
        return regimeService.creer(requete, auth.getName());
    }

    @GetMapping("/employes/{id}/plannings")
    public List<PlanningSalarieDto> plannings(@PathVariable Long id) {
        return regimeService.plannings(id);
    }

    @PostMapping("/employes/{id}/plannings")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanningSalarieDto affecterRegime(@PathVariable Long id,
                                             @Valid @RequestBody AffectationRegimeRequest requete,
                                             Authentication auth) {
        return regimeService.affecter(id, requete, auth.getName());
    }

    @GetMapping("/employes/{id}/semaine")
    public SemaineTempsDto semaine(@PathVariable Long id,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return tempsService.semaine(id, date);
    }

    @PostMapping("/pointages")
    @ResponseStatus(HttpStatus.CREATED)
    public SemaineTempsDto saisir(@Valid @RequestBody PointageRequest requete, Authentication auth) {
        return pointageService.saisir(requete, auth.getName());
    }

    @PutMapping("/pointages/{id}")
    public SemaineTempsDto corriger(@PathVariable Long id,
                                    @Valid @RequestBody CorrectionPointageRequest requete,
                                    Authentication auth) {
        return pointageService.corriger(id, requete, auth.getName());
    }

    @DeleteMapping("/pointages/{id}")
    public SemaineTempsDto supprimer(@PathVariable Long id, @RequestParam String motif, Authentication auth) {
        return pointageService.supprimer(id, motif, auth.getName());
    }

    @GetMapping("/heures-sup/a-valider")
    public List<SemaineTempsDto> heuresSupAValider() {
        return tempsService.heuresSupAValider();
    }

    @PostMapping("/heures-sup/decision")
    public SemaineTempsDto decider(@Valid @RequestBody DecisionHeuresSupRequest requete, Authentication auth) {
        return pointageService.deciderHeuresSup(requete, auth.getName());
    }

    @GetMapping("/anomalies")
    public List<AnomaliePointageDto> anomalies(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return tempsService.anomalies(du, au);
    }

    @GetMapping("/variables-paie")
    public List<VariablesPaieDto> variablesPaie(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) {
        return tempsService.variablesPaie(mois);
    }
}
