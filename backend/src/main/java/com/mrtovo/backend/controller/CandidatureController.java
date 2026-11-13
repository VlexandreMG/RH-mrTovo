package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.*;
import com.mrtovo.backend.service.CvStorageService;
import com.mrtovo.backend.service.RecrutementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
public class CandidatureController {

    private final RecrutementService service;

    @GetMapping("/offre/{offreId}")
    public List<CandidatureDTO> parOffre(@PathVariable Long offreId) {
        return service.candidaturesDeLOffre(offreId);
    }

    /** Candidats acceptés après entretien, à transmettre à Sarobidy. */
    @GetMapping("/acceptees")
    public List<CandidatureDTO> acceptees() {
        return service.candidaturesAcceptees();
    }

    @GetMapping("/{id}/cv")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Resource> cv(@PathVariable Long id) {
        CvStorageService.StoredCv cv = service.cvCandidature(id);
        String extension = cv.mediaType().equals(MediaType.APPLICATION_PDF) ? "pdf" : "docx";
        return ResponseEntity.ok()
                .contentType(cv.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("CV-candidature-" + id + "." + extension)
                                .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(cv.resource());
    }

    /** Historique de la candidature, du depot a la decision. Reserve au RH par SecurityConfig. */
    @GetMapping("/{id}/historique")
    public List<HistoriqueCandidatureDTO> historique(@PathVariable Long id) {
        return service.historique(id);
    }

    @GetMapping("/{id}")
    public CandidatureDTO get(@PathVariable Long id) {
        return service.getCandidature(id);
    }

    /** Présélection : { "valide": true } = retenue (passe au test), false = rejetée. */
    @PostMapping("/{id}/preselection")
    public CandidatureDTO preselection(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return service.preselectionner(id, req.valide());
    }

    /** Saisie de la note du test (0 à 20). */
    @PutMapping("/{id}/test/note")
    public CandidatureDTO note(@PathVariable Long id, @Valid @RequestBody NoteRequest req) {
        return service.saisirNote(id, req.note());
    }

    /** Décision du recruteur après avoir vu la note : { "valide": true|false }. */
    @PostMapping("/{id}/test/decision")
    public CandidatureDTO decisionTest(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return service.deciderTest(id, req.valide());
    }

    /** Planifie l'entretien : { "date": "2026-11-02T10:00", "lieu": "Salle 1" }. */
    @PostMapping("/{id}/entretiens")
    public CandidatureDTO planifierEntretien(@PathVariable Long id,
                                             @Valid @RequestBody PlanifierEntretienRequest req) {
        return service.planifierEntretien(id, req.date(), req.lieu());
    }
}
