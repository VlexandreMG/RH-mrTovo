package com.mrtovo.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Entity
@Table(name = "candidat", schema = "mrtovo")
@Data
public class Candidat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String prenom;
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@(gmail\\.com|proton\\.me|yahoo\\.[a-z]{2,})$", 
        message = "L'adresse email doit être un domaine valide (@gmail.com, @proton.me, @yahoo...)"
    )
    @Column(nullable = false, unique = true)
    private String email;
    private String telephone;
    private String fichierCvUrl;
}