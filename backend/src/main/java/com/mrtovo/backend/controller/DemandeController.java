package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.ApiDtos.DemandeDto;
import com.mrtovo.backend.dto.ApiDtos.DemandeRecrutementRequest;
import com.mrtovo.backend.dto.ApiDtos.ValidationRequest;
import com.mrtovo.backend.service.DemandeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** L'utilisateur est toujours identifié par son authentification, jamais par un paramètre de l'URL. */
@RestController
@RequestMapping("/api/demandes")
public class DemandeController {

    private final DemandeService demandeService;

    public DemandeController(DemandeService demandeService) {
        this.demandeService = demandeService;
    }

    @GetMapping
    public List<DemandeDto> lister(Authentication auth) {
        return demandeService.lister(auth.getName());
    }

    @GetMapping("/{id}")
    public DemandeDto detail(@PathVariable Long id, Authentication auth) {
        return demandeService.detail(id, auth.getName());
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public DemandeDto creer(@RequestBody DemandeRecrutementRequest request, Authentication auth) {
        return demandeService.creer(request, auth.getName());
    }

    @PostMapping("/{id}/validations")
    @PreAuthorize("hasAnyRole('RH','FINANCE','DIRECTION')")
    public DemandeDto valider(@PathVariable Long id, @RequestBody ValidationRequest request, Authentication auth) {
        return demandeService.valider(id, request, auth.getName());
    }

    @PostMapping("/{id}/ouverture")
    @PreAuthorize("hasRole('RH')")
    public DemandeDto ouvrir(@PathVariable Long id, Authentication auth) {
        return demandeService.ouvrir(id, auth.getName());
    }
}
