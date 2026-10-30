package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.CandidatureDTO;
import com.mrtovo.backend.dto.DecisionRequest;
import com.mrtovo.backend.service.RecrutementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entretiens")
@RequiredArgsConstructor
public class EntretienController {

    private final RecrutementService service;

    /** Résultat : { "valide": true, "commentaire": "..." } = accepté (→ Sarobidy), false = refusé. */
    @PostMapping("/{id}/resultat")
    public CandidatureDTO resultat(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return service.resultatEntretien(id, req.valide(), req.commentaire());
    }

    @PostMapping("/{id}/annulation")
    public CandidatureDTO annuler(@PathVariable Long id) {
        return service.annulerEntretien(id);
    }
}
