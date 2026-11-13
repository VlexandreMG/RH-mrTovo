package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.ReponsesTestRequest;
import com.mrtovo.backend.dto.TestCandidatDTO;
import com.mrtovo.backend.service.TestCandidatService;

import com.mrtovo.backend.service.EspaceCandidatService;
import com.mrtovo.backend.dto.NegociationDto;
import com.mrtovo.backend.dto.PropositionDto;
import com.mrtovo.backend.dto.RevendicationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/candidat")
public class EspaceCandidatController {

    private final EspaceCandidatService service;
    private final TestCandidatService testService;

    public EspaceCandidatController(EspaceCandidatService service, TestCandidatService testService) {
        this.service = service;
        this.testService = testService;
    }

    /** Bloc 2 : les tests du candidat connecté. */
    @GetMapping("/tests")
    public List<TestCandidatDTO.Resume> tests(Authentication authentication) {
        return testService.testsDuCandidat(authentication.getName());
    }

    @GetMapping("/tests/{candidatureId}")
    public TestCandidatDTO.Questionnaire test(Authentication authentication, @PathVariable Long candidatureId) {
        return testService.testAPasser(authentication.getName(), candidatureId);
    }

    @PostMapping("/tests/{candidatureId}/reponses")
    public TestCandidatDTO.Resume repondre(Authentication authentication, @PathVariable Long candidatureId,
                                           @Valid @RequestBody ReponsesTestRequest requete) {
        return testService.soumettre(authentication.getName(), candidatureId, requete);
    }

    @GetMapping("/propositions")
    public List<PropositionDto> propositions(Authentication authentication) {
        return service.propositions(authentication.getName());
    }

    @GetMapping("/propositions/{id}")
    public PropositionDto proposition(Authentication authentication, @PathVariable Long id) {
        return service.proposition(authentication.getName(), id);
    }

    @PostMapping("/propositions/{id}/accepter")
    public PropositionDto accepter(Authentication authentication, @PathVariable Long id,
                                   @Valid @RequestBody(required = false) ActionCandidatRequest request) {
        return service.accepter(authentication.getName(), id, request != null ? request.commentaire() : null);
    }

    @PostMapping("/propositions/{id}/refuser")
    public PropositionDto refuser(Authentication authentication, @PathVariable Long id,
                                  @Valid @RequestBody(required = false) ActionCandidatRequest request) {
        return service.refuser(authentication.getName(), id, request != null ? request.commentaire() : null);
    }

    @PostMapping("/propositions/{id}/revendiquer")
    public NegociationDto revendiquer(Authentication authentication, @PathVariable Long id,
                                      @Valid @RequestBody RevendicationRequest request) {
        return service.revendiquer(authentication.getName(), id, request);
    }

    public record ActionCandidatRequest(@Size(max = 255) String commentaire) {
    }
}
