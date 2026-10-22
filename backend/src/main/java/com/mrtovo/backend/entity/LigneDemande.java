package com.mrtovo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Une ligne = un niveau de poste demandé avec son effectif.
 * Le manager remplit niveau + effectif ; le RH renseigne ensuite la grille et le coût estimé.
 */
@Entity
@Table(name = "ligne_demande")
@Getter
@Setter
@NoArgsConstructor
public class LigneDemande {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne")
    private Long idLigne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_demande", nullable = false)
    private DemandeRecrutement demande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_niveau", nullable = false)
    private NiveauPoste niveau;

    @Column(nullable = false)
    private Integer effectif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_grille")
    private GrilleSalariale grille;

    @Column(name = "cout_estime_par_personne", precision = 12, scale = 2)
    private BigDecimal coutEstimeParPersonne;
}
