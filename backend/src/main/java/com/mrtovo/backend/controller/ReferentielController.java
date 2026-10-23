package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.ApiDtos.EmployeDto;
import com.mrtovo.backend.dto.ApiDtos.GrilleDto;
import com.mrtovo.backend.dto.ApiDtos.Ref;
import com.mrtovo.backend.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/referentiels")
public class ReferentielController {

    private final DepartementRepository departementRepository;
    private final PosteRepository posteRepository;
    private final NiveauPosteRepository niveauRepository;
    private final EmployeRepository employeRepository;
    private final GrilleSalarialeRepository grilleRepository;

    public ReferentielController(DepartementRepository departementRepository,
                                 PosteRepository posteRepository,
                                 NiveauPosteRepository niveauRepository,
                                 EmployeRepository employeRepository,
                                 GrilleSalarialeRepository grilleRepository) {
        this.departementRepository = departementRepository;
        this.posteRepository = posteRepository;
        this.niveauRepository = niveauRepository;
        this.employeRepository = employeRepository;
        this.grilleRepository = grilleRepository;
    }

    @GetMapping("/departements")
    public List<Ref> departements() {
        return departementRepository.findAll().stream()
                .map(d -> new Ref(d.getIdDepartement(), d.getNom())).toList();
    }

    @GetMapping("/postes")
    public List<Ref> postes() {
        return posteRepository.findAll().stream()
                .map(p -> new Ref(p.getIdPoste(), p.getIntitule())).toList();
    }

    @GetMapping("/niveaux")
    public List<Ref> niveaux() {
        return niveauRepository.findAllByOrderByRangAsc().stream()
                .map(n -> new Ref(n.getIdNiveau(), n.getLibelle())).toList();
    }

    @GetMapping("/employes")
    @Transactional(readOnly = true)
    public List<EmployeDto> employes() {
        return employeRepository.findByActifTrueOrderByNomAsc().stream()
                .map(e -> new EmployeDto(e.getIdEmploye(), e.getNom(), e.getMatricule(),
                        e.getPoste() == null ? null : e.getPoste().getIdPoste(),
                        e.getPoste() == null ? null : e.getPoste().getIntitule(),
                        e.getNiveau() == null ? null : e.getNiveau().getLibelle(),
                        e.getDepartement() == null ? null : e.getDepartement().getNom()))
                .toList();
    }

    @GetMapping("/grilles")
    @Transactional(readOnly = true)
    public List<GrilleDto> grilles() {
        return grilleRepository.findAllByOrderByPosteIntituleAscNiveauRangAscDateDebutDesc().stream()
                .map(g -> new GrilleDto(g.getIdGrille(),
                        g.getPoste().getIdPoste(), g.getPoste().getIntitule(),
                        g.getNiveau().getIdNiveau(), g.getNiveau().getLibelle(),
                        g.getSalaireMin(), g.getSalaireMax(), g.getDateDebut(), g.getDateFin()))
                .toList();
    }
}
