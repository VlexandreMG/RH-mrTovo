-- Schéma PostgreSQL de référence (Hibernate le crée automatiquement avec ddl-auto=update).
-- Pour repartir de zéro :  DROP SCHEMA public CASCADE; CREATE SCHEMA public;

CREATE TABLE departement (
    id_departement BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom            VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE poste (
    id_poste    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    intitule    VARCHAR(150) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE niveau_poste (
    id_niveau BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    libelle   VARCHAR(50) NOT NULL UNIQUE,
    rang      INTEGER NOT NULL
);

CREATE TABLE utilisateur (
    id_utilisateur BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom            VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe   VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('MANAGER','RH','FINANCE','DIRECTION'))
);

CREATE TABLE employe (
    id_employe     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom            VARCHAR(150) NOT NULL,
    matricule      VARCHAR(50)  NOT NULL UNIQUE,
    id_poste       BIGINT REFERENCES poste(id_poste),
    id_niveau      BIGINT REFERENCES niveau_poste(id_niveau),
    id_departement BIGINT REFERENCES departement(id_departement),
    date_embauche  DATE,
    actif          BOOLEAN NOT NULL DEFAULT TRUE
);

-- Fourchette salariale mensuelle par poste ET par niveau, avec période de validité
CREATE TABLE grille_salariale (
    id_grille   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_poste    BIGINT NOT NULL REFERENCES poste(id_poste),
    id_niveau   BIGINT NOT NULL REFERENCES niveau_poste(id_niveau),
    salaire_min NUMERIC(12,2) NOT NULL CHECK (salaire_min >= 0),
    salaire_max NUMERIC(12,2) NOT NULL,
    date_debut  DATE NOT NULL,
    date_fin    DATE,
    CHECK (salaire_max >= salaire_min),
    CHECK (date_fin IS NULL OR date_fin >= date_debut),
    UNIQUE (id_poste, id_niveau, date_debut)
);

CREATE TABLE demande_recrutement (
    id_demande           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_poste             BIGINT NOT NULL REFERENCES poste(id_poste),
    id_departement       BIGINT NOT NULL REFERENCES departement(id_departement),
    id_manager           BIGINT NOT NULL REFERENCES utilisateur(id_utilisateur),
    type_besoin          VARCHAR(20) NOT NULL CHECK (type_besoin IN ('CREATION','REMPLACEMENT')),
    motif                TEXT NOT NULL,                      -- raison métier en texte libre
    id_employe_remplace  BIGINT REFERENCES employe(id_employe),
    budget_demande_total NUMERIC(14,2) NOT NULL CHECK (budget_demande_total >= 0),
    date_souhaitee       DATE NOT NULL,
    statut               VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE_RH' CHECK (statut IN
        ('EN_ATTENTE_RH','EN_ATTENTE_FINANCE','EN_ATTENTE_DIRECTION','REFUSEE','AUTORISEE','RECRUTEMENT_OUVERT')),
    date_creation        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_ouverture       TIMESTAMP
);

-- Une ligne par niveau demandé (ex. 2 juniors + 1 senior)
CREATE TABLE ligne_demande (
    id_ligne                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_demande               BIGINT NOT NULL REFERENCES demande_recrutement(id_demande) ON DELETE CASCADE,
    id_niveau                BIGINT NOT NULL REFERENCES niveau_poste(id_niveau),
    effectif                 INTEGER NOT NULL CHECK (effectif > 0),
    id_grille                BIGINT REFERENCES grille_salariale(id_grille),   -- renseignée par le RH
    cout_estime_par_personne NUMERIC(12,2) CHECK (cout_estime_par_personne >= 0),
    UNIQUE (id_demande, id_niveau)
);

-- Historique : une ligne par avis, jamais écrasée
CREATE TABLE validation (
    id_validation   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_demande      BIGINT NOT NULL REFERENCES demande_recrutement(id_demande),
    id_validateur   BIGINT NOT NULL REFERENCES utilisateur(id_utilisateur),
    etape           VARCHAR(20) NOT NULL CHECK (etape IN ('RH','FINANCE','DIRECTION')),
    decision        VARCHAR(20) NOT NULL CHECK (decision IN ('FAVORABLE','DEFAVORABLE')),
    commentaire     TEXT,
    date_validation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
