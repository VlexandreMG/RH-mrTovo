package com.mrtovo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "departement")
@Getter
@Setter
@NoArgsConstructor
public class Departement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departement")
    private Long idDepartement;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;
}
