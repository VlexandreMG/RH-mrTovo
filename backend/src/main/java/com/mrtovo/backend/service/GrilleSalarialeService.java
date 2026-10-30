package com.mrtovo.backend.service;

import com.mrtovo.backend.entity.GrilleSalarialeRecrutement;
import com.mrtovo.backend.repository.GrilleRecrutementRepository;
import com.mrtovo.backend.dto.GrilleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GrilleSalarialeService {

    private final GrilleRecrutementRepository repository;

    public GrilleSalarialeService(GrilleRecrutementRepository repository) {
        this.repository = repository;
    }

    public List<GrilleDto> parPoste(Long posteId) {
        if (posteId == null) {
            return List.of();
        }
        return repository.findByPosteIdOrderBySalaireMinAsc(posteId).stream().map(this::toDto).toList();
    }

    public GrilleDto niveauPourSalaire(Long posteId, BigDecimal salaire, LocalDate reference) {
        if (posteId == null || salaire == null) {
            return null;
        }
        List<GrilleSalarialeRecrutement> grilles = repository.findByPosteIdOrderBySalaireMinAsc(posteId);
        return grilles.stream()
                .filter(grille -> grille.contient(salaire))
                .filter(grille -> grille.estActive(reference))
                .findFirst()
                .map(this::toDto)
                .orElse(null);
    }

    public boolean postePossedeUneGrille(Long posteId) {
        return posteId != null && !repository.findByPosteIdOrderBySalaireMinAsc(posteId).isEmpty();
    }

    public GrilleDto toDto(GrilleSalarialeRecrutement grille) {
        return new GrilleDto(
                grille.getId(),
                grille.getPoste() != null ? grille.getPoste().getId() : null,
                grille.getPoste() != null ? grille.getPoste().getTitre() : null,
                grille.getNiveau(),
                grille.getSalaireMin(),
                grille.getSalaireMax(),
                grille.getDateDebut(),
                grille.getDateFin());
    }
}
