package com.mrtovo.backend.bloc3.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class IdentifiantService {

    private final JdbcTemplate jdbcTemplate;

    public IdentifiantService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String prochainMatricule() {
        return "EMP-" + Year.now().getValue() + "-" + suffixeSequence("mrtovo.seq_matricule", "EMP");
    }

    public String prochaineReferenceContrat() {
        return "CTR-" + Year.now().getValue() + "-" + suffixeSequence("mrtovo.seq_contrat_reference", "CTR");
    }

    private String suffixeSequence(String sequence, String prefixe) {
        Long valeur = jdbcTemplate.queryForObject("select nextval('" + sequence + "')", Long.class);
        if (valeur == null) {
            throw new IllegalStateException("Sequence " + sequence + " indisponible");
        }
        return String.format("%04d", valeur);
    }
}
