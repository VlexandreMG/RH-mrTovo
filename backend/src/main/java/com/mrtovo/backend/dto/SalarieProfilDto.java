package com.mrtovo.backend.dto;

import java.util.List;

public record SalarieProfilDto(
        String matricule,
        String nomComplet,
        String posteTitre,
        String departementNom,
        SoldeCongeDto solde,
        List<TypeCongeDto> types,
        boolean soldeSuivi) {
}
