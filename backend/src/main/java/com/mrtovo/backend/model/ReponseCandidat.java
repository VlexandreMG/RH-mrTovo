package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Le choix d'un candidat pour une question. Une seule reponse par question et par test. */
@Entity
@Table(name = "reponse_candidat", schema = "mrtovo")
@Getter
@Setter
public class ReponseCandidat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionTest question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "choix_id", nullable = false)
    private ChoixQuestion choix;
}
