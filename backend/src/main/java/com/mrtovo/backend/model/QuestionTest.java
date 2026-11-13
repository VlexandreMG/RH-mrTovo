package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Une question du QCM d'une offre, ecrite par le RH. */
@Entity
@Table(name = "question_test", schema = "mrtovo")
@Getter
@Setter
public class QuestionTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_emploi_id", nullable = false)
    private OffreEmploi offreEmploi;

    @Column(nullable = false)
    private Integer ordre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enonce;

    /** Les choix vivent et meurent avec leur question. */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    private List<ChoixQuestion> choix = new ArrayList<>();
}
