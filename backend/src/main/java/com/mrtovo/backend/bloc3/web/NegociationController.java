package com.mrtovo.backend.bloc3.web;

import com.mrtovo.backend.bloc3.service.NegociationService;
import com.mrtovo.backend.bloc3.web.dto.ActionRequest;
import com.mrtovo.backend.bloc3.web.dto.DecisionNegociationRequest;
import com.mrtovo.backend.bloc3.web.dto.NegociationDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bloc3/negotiations")
public class NegociationController {

    private final NegociationService negociationService;

    public NegociationController(NegociationService negociationService) {
        this.negociationService = negociationService;
    }

    @GetMapping
    public List<NegociationDto> lister() {
        return negociationService.lister();
    }

    @GetMapping("/{id}")
    public NegociationDto detail(@PathVariable Long id) {
        return negociationService.detail(id);
    }

    @PostMapping("/{id}/analyser")
    public NegociationDto analyser(@PathVariable Long id, @RequestBody(required = false) ActionRequest request) {
        ActionRequest requete = request != null ? request : new ActionRequest(null, null);
        return negociationService.analyser(id, requete);
    }

    @PostMapping("/{id}/decider")
    public NegociationDto decider(@PathVariable Long id,
                                  @Valid @RequestBody DecisionNegociationRequest request) {
        return negociationService.decider(id, request);
    }
}
