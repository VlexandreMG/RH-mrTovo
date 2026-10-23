package com.mrtovo.backend.dto;

import java.time.LocalDate;

public record OffreDTO(Long id, String titre, String description, LocalDate datePublication,
                       String statut, long nbCandidatures) {}
