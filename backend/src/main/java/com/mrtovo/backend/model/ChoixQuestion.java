package com.mrtovo.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Un choix de reponse. Une seule bonne reponse par question (index unique partiel en base). */
@Entity
@Table(name = "choix_question", schema = "mrtovo")
@Getter
@Setter
public class ChoixQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionTest question;

    @Column(nullable = false)
    private Integer ordre;

    @Column(nullable = false, length = 500)
    private String libelle;

    /** Ne quitte jamais le serveur vers un candidat. */
    @Column(nullable = false)
    private boolean correct;
}
