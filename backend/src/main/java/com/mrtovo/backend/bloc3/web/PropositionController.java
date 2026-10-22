package com.mrtovo.backend.bloc3.web;

import com.mrtovo.backend.bloc3.service.PropositionService;
import com.mrtovo.backend.bloc3.service.NegociationService;
import com.mrtovo.backend.bloc3.web.dto.AcceptionRequest;
import com.mrtovo.backend.bloc3.web.dto.ActionRequest;
import com.mrtovo.backend.bloc3.web.dto.EmissionRequest;
import com.mrtovo.backend.bloc3.web.dto.NegociationDto;
import com.mrtovo.backend.bloc3.web.dto.PropositionCreateRequest;
import com.mrtovo.backend.bloc3.web.dto.PropositionDto;
import com.mrtovo.backend.bloc3.web.dto.RevendicationRequest;
import com.mrtovo.backend.bloc3.web.dto.ValidationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bloc3/propositions")
public class PropositionController {

    private final PropositionService propositionService;
    private final NegociationService negociationService;

    public PropositionController(PropositionService propositionService, NegociationService negociationService) {
        this.propositionService = propositionService;
        this.negociationService = negociationService;
    }

    @GetMapping
    public List<PropositionDto> lister(@RequestParam(required = false) String statut,
                                       @RequestParam(required = false) Long candidatureId) {
        return propositionService.lister(statut, candidatureId);
    }

    @GetMapping("/{id}")
    public PropositionDto detail(@PathVariable Long id) {
        return propositionService.detail(id);
    }

    @PostMapping
    public ResponseEntity<PropositionDto> creer(@Valid @RequestBody PropositionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propositionService.creer(request));
    }

    @PostMapping("/{id}/valider")
    public PropositionDto valider(@PathVariable Long id, @Valid @RequestBody ValidationRequest request) {
        return propositionService.valider(id, request);
    }

    @PostMapping("/{id}/emettre")
    public PropositionDto emettre(@PathVariable Long id, @RequestBody(required = false) EmissionRequest request) {
        EmissionRequest requete = request != null ? request : new EmissionRequest(null, null, null);
        return propositionService.emettre(id, requete);
    }

    @PostMapping("/{id}/accepter")
    public PropositionDto accepter(@PathVariable Long id, @RequestBody(required = false) AcceptionRequest request) {
        AcceptionRequest requete = request != null ? request : new AcceptionRequest(null, null, null);
        return propositionService.accepter(id, requete);
    }

    @PostMapping("/{id}/refuser")
    public PropositionDto refuser(@PathVariable Long id, @RequestBody(required = false) ActionRequest request) {
        ActionRequest requete = request != null ? request : new ActionRequest(null, null);
        return propositionService.refuser(id, requete);
    }

    @PostMapping("/{id}/annuler")
    public PropositionDto annuler(@PathVariable Long id, @RequestBody(required = false) ActionRequest request) {
        ActionRequest requete = request != null ? request : new ActionRequest(null, null);
        return negociationService.annulerProposition(id, requete);
    }

    @PostMapping("/{id}/revoquer")
    public NegociationDto revoquer(@PathVariable Long id, @Valid @RequestBody RevendicationRequest request) {
        return negociationService.deposerRevendication(id, request);
    }
}
