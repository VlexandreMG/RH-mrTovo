package com.mrtovo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Niveau de qualification : Junior, Confirmé, Senior... */
@Entity
@Table(name = "niveau_poste")
@Getter
@Setter
@NoArgsConstructor
public class NiveauPoste {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_niveau")
    private Long idNiveau;

    @Column(nullable = false, unique = true, length = 50)
    private String libelle;

    @Column(nullable = false)
    private Integer rang = 1;
}
