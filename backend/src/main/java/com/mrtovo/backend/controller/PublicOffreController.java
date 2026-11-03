package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.CandidatureDTO;
import com.mrtovo.backend.dto.OffreDTO;
import com.mrtovo.backend.service.RecrutementService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/public/offres")
@RequiredArgsConstructor
@Validated
public class PublicOffreController {

    private final RecrutementService service;

    @GetMapping
    public List<OffreDTO> lister() {
        return service.listerOffresPubliques();
    }

    @GetMapping("/{id}")
    public OffreDTO get(@PathVariable Long id) {
        return service.getOffrePublique(id);
    }

    @PostMapping("/{id}/candidatures")
    public ResponseEntity<CandidatureDTO> postuler(
            @PathVariable Long id,
            @RequestParam("nom") @NotBlank @Size(max = 255) String nom,
            @RequestParam("prenom") @NotBlank @Size(max = 255) String prenom,
            @RequestParam("email") @NotBlank @Email @Size(max = 255) String email,
            @RequestParam(value = "telephone", required = false) @Size(max = 50) String telephone,
            @RequestPart("cv") MultipartFile cv) {
        CandidatureDTO candidature = service.postuler(id, nom, prenom, email, telephone, cv);
        return ResponseEntity.status(HttpStatus.CREATED).body(candidature);
    }

}
