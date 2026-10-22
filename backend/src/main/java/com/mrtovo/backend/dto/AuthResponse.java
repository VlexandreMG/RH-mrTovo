package com.mrtovo.backend.dto;

public record AuthResponse(Long idUtilisateur, String nom, String email, String role, String departement) {
}