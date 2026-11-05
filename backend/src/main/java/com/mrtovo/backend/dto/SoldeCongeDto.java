package com.mrtovo.backend.dto;

import java.math.BigDecimal;

/** SoldeConge du cours : il n'est pas stocke, il se deduit des droits et des demandes. */
public record SoldeCongeDto(
        Long employeId,
        String matricule,
        String nomComplet,
        int moisComplets,
        BigDecimal joursAcquis,
        BigDecimal joursAjustes,
        BigDecimal joursPris,
        BigDecimal joursIndemnises,
        BigDecimal solde,
        BigDecimal joursEnAttente,
        BigDecimal disponible,
        BigDecimal salaireBrut,
        BigDecimal indemniteCompensatrice,
        boolean compteActif) {
}
