package com.mrtovo.backend.bloc3.web.dto;

import java.util.List;
import java.util.Map;

public record DashboardDto(
        long candidaturesRetenues,
        long propositionsEnCours,
        long propositionsEmises,
        long propositionsAcceptees,
        long negotiationsEnCours,
        long employesCrees,
        long contratsASigner,
        Map<String, Long> repartitionPropositions,
        Map<String, Long> repartitionNegociations,
        List<PropositionDto> dernieresPropositions) {
}
