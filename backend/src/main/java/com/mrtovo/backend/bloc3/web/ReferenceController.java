package com.mrtovo.backend.bloc3.web;

import com.mrtovo.backend.bloc3.domain.Departement;
import com.mrtovo.backend.bloc3.domain.OffreEmploi;
import com.mrtovo.backend.bloc3.domain.Poste;
import com.mrtovo.backend.bloc3.repository.DepartementRepository;
import com.mrtovo.backend.bloc3.repository.OffreEmploiRepository;
import com.mrtovo.backend.bloc3.repository.PosteRepository;
import com.mrtovo.backend.bloc3.service.GrilleSalarialeService;
import com.mrtovo.backend.bloc3.web.dto.DepartementDto;
import com.mrtovo.backend.bloc3.web.dto.GrilleDto;
import com.mrtovo.backend.bloc3.web.dto.OffreEmploiDto;
import com.mrtovo.backend.bloc3.web.dto.PosteDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bloc3/reference")
public class ReferenceController {

    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final OffreEmploiRepository offreEmploiRepository;
    private final GrilleSalarialeService grilleSalarialeService;

    public ReferenceController(PosteRepository posteRepository,
                               DepartementRepository departementRepository,
                               OffreEmploiRepository offreEmploiRepository,
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

    private PosteDto toDto(Poste poste) {
        return new PosteDto(poste.getId(), poste.getTitre(), poste.getDescription(),
                poste.getDepartement() != null ? poste.getDepartement().getId() : null,
                poste.getDepartement() != null ? poste.getDepartement().getNom() : null,
                poste.getStatut().name());
    }

    private DepartementDto toDto(Departement departement) {
        return new DepartementDto(departement.getId(), departement.getNom(), departement.getCode());
    }

    private OffreEmploiDto toDto(OffreEmploi offre) {
        return new OffreEmploiDto(offre.getId(), offre.getTitre(), offre.getDescription(),
                offre.getDatePublication(), offre.getStatut().name());
    }
}
