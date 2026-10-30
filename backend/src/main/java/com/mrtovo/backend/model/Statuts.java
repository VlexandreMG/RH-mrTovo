package com.mrtovo.backend.model;

/** Identifiants des statuts insérés par data.sql. */
public final class Statuts {
    private Statuts() {}

    public static final class Candidature {
        public static final long A_TRAITER = 1;
        public static final long TEST = 2;
        public static final long ATTENTE_ENTRETIEN = 3;
        public static final long ACCEPTE = 4;
        public static final long REFUSE = 5;
        private Candidature() {}
    }

    public static final class OffreEmploi {
        public static final long OUVERTE = 1;
        public static final long FERMEE = 2;
        /** Créée avec la demande, en attente de l'ouverture du recrutement. */
        public static final long EN_ATTENTE = 3;
        private OffreEmploi() {}
    }

    public static final class Evaluation {
        public static final long EN_ATTENTE = 1;
        public static final long VALIDE = 2;
        public static final long ECHOUE = 3;
        private Evaluation() {}
    }

    public static final class Entretien {
        public static final long PLANIFIE = 1;
        public static final long TERMINE = 2;
        public static final long ANNULE = 3;
        private Entretien() {}
    }
}
