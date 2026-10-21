package com.mrtovo.backend.bloc3.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.UUID;

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
        try {
            Long valeur = jdbcTemplate.queryForObject("select nextval('" + sequence + "')", Long.class);
            return String.format("%04d", valeur == null ? 0 : valeur);
        } catch (RuntimeException exception) {
            return prefixe + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}
