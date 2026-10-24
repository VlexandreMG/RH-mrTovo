package com.mrtovo.backend.bloc3.web.dto;

import com.mrtovo.backend.bloc3.domain.StatutNegociation;
import com.mrtovo.backend.bloc3.domain.TypeNegociation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NegociationDto(
        Long id,
        Long propositionId,
        Integer propositionVersion,
        Long candidatId,
        String candidatNom,
        String candidatPrenom,
        TypeNegociation type,
        BigDecimal salaireRevendique,
        String avantageRevendique,
        String motif,
        LocalDate dateDemande,
        StatutNegociation statut,
        String decidePar,
        LocalDate dateDecision,
        String commentaireDecision,
        Long propositionResultanteId,
        List<HistoriqueDto> historique) {
}
