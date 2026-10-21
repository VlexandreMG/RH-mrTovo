package com.mrtovo.backend.bloc3.web.dto;

import java.time.LocalDateTime;

public record HistoriqueDto(
        Long id,
        String ancienStatut,
        String nouveauStatut,
        String action,
        String auteur,
        String commentaire,
        LocalDateTime dateChangement) {
}
