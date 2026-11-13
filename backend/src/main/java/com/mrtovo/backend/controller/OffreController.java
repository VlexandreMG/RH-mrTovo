package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.QuestionnaireDTO;
import com.mrtovo.backend.dto.QuestionnaireRequest;
import com.mrtovo.backend.service.TestCandidatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mrtovo.backend.dto.OffreDTO;
import com.mrtovo.backend.service.RecrutementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
public class OffreController {

    private final RecrutementService service;
    private final TestCandidatService testService;

    @GetMapping
    public List<OffreDTO> lister() {
        return service.listerOffres();
    }

    @GetMapping("/{id}")
    public OffreDTO get(@PathVariable Long id) {
        return service.getOffre(id);
    }

    /** QCM de l'offre, bonnes réponses comprises. Réservé au RH par SecurityConfig. */
    @GetMapping("/{id}/questionnaire")
    public QuestionnaireDTO questionnaire(@PathVariable Long id) {
        return testService.questionnaire(id);
    }

    /** Remplace le QCM ; refusé dès qu'un candidat a répondu. */
    @PutMapping("/{id}/questionnaire")
    public QuestionnaireDTO enregistrerQuestionnaire(@PathVariable Long id,
                                                     @Valid @RequestBody QuestionnaireRequest requete) {
        return testService.enregistrerQuestionnaire(id, requete);
    }
}
