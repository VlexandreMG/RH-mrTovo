package com.mrtovo.backend.controller;

import com.mrtovo.backend.entity.DepartementRecrutement;
import com.mrtovo.backend.entity.OffreRecrutement;
import com.mrtovo.backend.entity.PosteRecrutement;
import com.mrtovo.backend.repository.DepartementRecrutementRepository;
import com.mrtovo.backend.repository.OffreRecrutementRepository;
import com.mrtovo.backend.repository.PosteRecrutementRepository;
import com.mrtovo.backend.service.GrilleSalarialeService;
import com.mrtovo.backend.dto.DepartementDto;
import com.mrtovo.backend.dto.GrilleDto;
import com.mrtovo.backend.dto.OffreEmploiDto;
import com.mrtovo.backend.dto.PosteDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bloc3/reference")
public class ReferenceController {

    private final PosteRecrutementRepository posteRepository;
    private final DepartementRecrutementRepository departementRepository;
    private final OffreRecrutementRepository offreEmploiRepository;
    private final GrilleSalarialeService grilleSalarialeService;

    public ReferenceController(PosteRecrutementRepository posteRepository,
                               DepartementRecrutementRepository departementRepository,
                               OffreRecrutementRepository offreEmploiRepository,
                               GrilleSalarialeService grilleSalarialeService) {
        this.posteRepository = posteRepository;
        this.departementRepository = departementRepository;
        this.offreEmploiRepository = offreEmploiRepository;
        this.grilleSalarialeService = grilleSalarialeService;
    }

    @GetMapping("/postes")
    public List<PosteDto> postes() {
        return posteRepository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/departements")
    public List<DepartementDto> departements() {
        return departementRepository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/offres")
    public List<OffreEmploiDto> offres() {
        return offreEmploiRepository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/grilles/{posteId}")
    public List<GrilleDto> grilles(@PathVariable Long posteId) {
        return grilleSalarialeService.parPoste(posteId);
    }

    private PosteDto toDto(PosteRecrutement poste) {
        return new PosteDto(poste.getId(), poste.getTitre(), poste.getDescription(),
                poste.getDepartement() != null ? poste.getDepartement().getId() : null,
                poste.getDepartement() != null ? poste.getDepartement().getNom() : null,
                poste.getStatut().name());
    }

    private DepartementDto toDto(DepartementRecrutement departement) {
        return new DepartementDto(departement.getId(), departement.getNom(), departement.getCode());
    }

    private OffreEmploiDto toDto(OffreRecrutement offre) {
        return new OffreEmploiDto(offre.getId(), offre.getTitre(), offre.getDescription(),
                offre.getDatePublication(), offre.getStatut().name());
    }
}
