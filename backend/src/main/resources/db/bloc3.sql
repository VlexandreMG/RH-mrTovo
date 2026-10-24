-- =====================================================================
-- MRTOVO - Bloc 3 : SELECTION ET EMBAUCHE
-- Schéma SQL de référence (compatible PostgreSQL 13+)
--
-- Le script est idempotent : il peut être rejoué sans risque.
--   psql -h 127.0.0.1 -U mrtovo -d rh_db -f bloc3.sql
--
-- Périmètre : objets du Bloc 3 + le socle minimal du Bloc 1/2
-- (candidat, candidature, offre_emploi, poste, departement) afin que
-- l'application Bloc 3 soit utilisable de facon autonome.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS mrtovo;
SET search_path TO mrtovo;

-- ---------------------------------------------------------------------
-- SOCLE BLOC 1 / BLOC 2 (cree uniquement si absent)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mrtovo.departement (
    id          BIGSERIAL PRIMARY KEY,
    nom         VARCHAR(255) NOT NULL UNIQUE,
    code        VARCHAR(50) UNIQUE
);

CREATE TABLE IF NOT EXISTS mrtovo.poste (
    id              BIGSERIAL PRIMARY KEY,
    titre           VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    departement_id  BIGINT REFERENCES mrtovo.departement(id),
    statut          VARCHAR(30) NOT NULL DEFAULT 'ACTIF'
);

CREATE TABLE IF NOT EXISTS mrtovo.candidat (
    id             BIGSERIAL PRIMARY KEY,
    nom            VARCHAR(255) NOT NULL,
    prenom         VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    telephone      VARCHAR(50),
    fichier_cv_url VARCHAR(250)
);

CREATE TABLE IF NOT EXISTS mrtovo.offre_emploi (
    id              BIGSERIAL PRIMARY KEY,
    titre           VARCHAR(255) NOT NULL,
    description     VARCHAR(255) NOT NULL,
    date_publication DATE NOT NULL,
    date_cloture     DATE,
    statut          VARCHAR(30) NOT NULL DEFAULT 'BROUILLON'
);

CREATE TABLE IF NOT EXISTS mrtovo.candidature (
    id                BIGSERIAL PRIMARY KEY,
    candidat_id       BIGINT NOT NULL REFERENCES mrtovo.candidat(id),
    offre_emploi_id   BIGINT NOT NULL REFERENCES mrtovo.offre_emploi(id),
    date_candidature  DATE NOT NULL,
    date_statut       DATE,
    commentaire       VARCHAR(255),
    statut            VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    CONSTRAINT uq_candidature_candidat_offre UNIQUE (candidat_id, offre_emploi_id)
);

-- ---------------------------------------------------------------------
-- BLOC 3 - 1. GRILLE SALARIALE (par poste et par niveau)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mrtovo.grille_salariale (
    id          BIGSERIAL PRIMARY KEY,
    poste_id    BIGINT NOT NULL REFERENCES mrtovo.poste(id),
    niveau      VARCHAR(50) NOT NULL,
    salaire_min NUMERIC(12,2) NOT NULL,
    salaire_max NUMERIC(12,2) NOT NULL,
    date_debut  DATE NOT NULL,
    date_fin    DATE,
    CONSTRAINT uq_grille_salariale UNIQUE (poste_id, niveau, date_debut),
    CONSTRAINT ck_grille_salariale_bornes CHECK (salaire_min <= salaire_max)
);

-- ---------------------------------------------------------------------
-- BLOC 3 - 2. PROPOSITION D'EMBAUCHE
-- Une proposition est TOUJOURS rattachee a une candidature (Bloc 2).
-- Chaque negociation acceptee cree une nouvelle version (version + 1)
-- de la proposition : l'historique n'est jamais ecrase.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mrtovo.proposition_offre (
    id                   BIGSERIAL PRIMARY KEY,
    candidature_id       BIGINT NOT NULL REFERENCES mrtovo.candidature(id),
    version              INTEGER NOT NULL DEFAULT 1,
    proposition_parent_id BIGINT REFERENCES mrtovo.proposition_offre(id),
    salaire_base         NUMERIC(12,2) NOT NULL,
    temps_travail        VARCHAR(100) NOT NULL,
    emploi_du_temps      VARCHAR(255),
    lieu_travail         VARCHAR(255) NOT NULL,
    date_prise_poste     DATE NOT NULL,
    date_limite_validite DATE NOT NULL,
    date_emission        DATE NOT NULL,
    date_fin_contrat     DATE,
    type_contrat         VARCHAR(30) NOT NULL DEFAULT 'CDI',
    periode_essai_mois   INTEGER NOT NULL DEFAULT 0,
    poste_id             BIGINT REFERENCES mrtovo.poste(id),
    departement_id       BIGINT REFERENCES mrtovo.departement(id),
    statut               VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
    motif_revision       VARCHAR(255),
    validee_par          VARCHAR(255),
    date_validation      DATE,
    date_acceptation     DATE,
    CONSTRAINT uq_proposition_offre_version UNIQUE (candidature_id, version)
);

ALTER TABLE mrtovo.proposition_offre ADD COLUMN IF NOT EXISTS date_fin_contrat DATE;

-- ---------------------------------------------------------------------
-- BLOC 3 - 3. AVANTAGES DE L'OFFRE (mutuelle, equipement, ...)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mrtovo.avantage_offre (
    id            BIGSERIAL PRIMARY KEY,
    proposition_id BIGINT NOT NULL REFERENCES mrtovo.proposition_offre(id) ON DELETE CASCADE,
    type          VARCHAR(50) NOT NULL,
    libelle       VARCHAR(255) NOT NULL,
    description   VARCHAR(255),
    montant       NUMERIC(12,2),
    ordre         INTEGER NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------
-- BLOC 3 - 4. NEGOCIATION / REVENDICATION DU CANDIDAT
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mrtovo.negociation (
    id                       BIGSERIAL PRIMARY KEY,
    proposition_id           BIGINT NOT NULL REFERENCES mrtovo.proposition_offre(id),
    candidat_id              BIGINT NOT NULL REFERENCES mrtovo.candidat(id),
    type                     VARCHAR(50) NOT NULL DEFAULT 'REVENDICATION',
    salaire_revendique       NUMERIC(12,2),
    avantage_revendique      VARCHAR(255),
    motif                    VARCHAR(255) NOT NULL,
    date_demande             DATE NOT NULL,
    statut                   VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    decide_par               VARCHAR(255),
    date_decision            DATE,
    commentaire_decision     VARCHAR(255),
    proposition_resultante_id BIGINT REFERENCES mrtovo.proposition_offre(id)
);

-- ---------------------------------------------------------------------
-- BLOC 3 - 5. TRACABILITE (historique des statuts)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mrtovo.historique_proposition (
    id             BIGSERIAL PRIMARY KEY,
    proposition_id BIGINT NOT NULL REFERENCES mrtovo.proposition_offre(id) ON DELETE CASCADE,
    ancien_statut  VARCHAR(30),
    nouveau_statut VARCHAR(30) NOT NULL,
    action         VARCHAR(50) NOT NULL,
    auteur         VARCHAR(255),
    commentaire    VARCHAR(255),
    date_changement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mrtovo.historique_negociation (
    id             BIGSERIAL PRIMARY KEY,
    negociation_id BIGINT NOT NULL REFERENCES mrtovo.negociation(id) ON DELETE CASCADE,
    ancien_statut  VARCHAR(30),
    nouveau_statut VARCHAR(30) NOT NULL,
    action         VARCHAR(50) NOT NULL,
    auteur         VARCHAR(255),
    commentaire    VARCHAR(255),
    date_changement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- BLOC 3 - 6. EMPLOYE (le candidat devient salarie : CANDIDAT != EMPLOYE)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mrtovo.employe (
    id                    BIGSERIAL PRIMARY KEY,
    matricule             VARCHAR(50) NOT NULL UNIQUE,
    candidat_id           BIGINT NOT NULL UNIQUE REFERENCES mrtovo.candidat(id),
    proposition_id        BIGINT NOT NULL UNIQUE REFERENCES mrtovo.proposition_offre(id),
    nom                   VARCHAR(255) NOT NULL,
    prenom                VARCHAR(255) NOT NULL,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    telephone             VARCHAR(50),
    adresse               VARCHAR(255),
    poste_id              BIGINT REFERENCES mrtovo.poste(id),
    departement_id        BIGINT REFERENCES mrtovo.departement(id),
    date_embauche         DATE NOT NULL,
    date_entree           DATE NOT NULL,
    date_fin_contrat      DATE,
    date_fin_periode_essai DATE,
    statut                VARCHAR(30) NOT NULL DEFAULT 'EN_PERIODE_DESSAI'
);

-- ---------------------------------------------------------------------
-- BLOC 3 - 7. CONTRAT (historique conserve : plusieurs contrats successifs)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mrtovo.contrat (
    id            BIGSERIAL PRIMARY KEY,
    employe_id    BIGINT NOT NULL REFERENCES mrtovo.employe(id),
    reference     VARCHAR(50) NOT NULL UNIQUE,
    type_contrat  VARCHAR(30) NOT NULL,
    date_debut    DATE NOT NULL,
    date_fin      DATE,
    salaire       NUMERIC(12,2) NOT NULL,
    statut_contrat VARCHAR(30) NOT NULL DEFAULT 'A_SIGNER',
    document_url  VARCHAR(255)
);

-- ---------------------------------------------------------------------
-- BLOC 3 - 8. AFFECTATION (poste + departement dans le temps)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mrtovo.affectation (
    id             BIGSERIAL PRIMARY KEY,
    employe_id     BIGINT NOT NULL REFERENCES mrtovo.employe(id),
    poste_id       BIGINT NOT NULL REFERENCES mrtovo.poste(id),
    departement_id BIGINT REFERENCES mrtovo.departement(id),
    type           VARCHAR(50) NOT NULL DEFAULT 'INITIALE',
    date_debut     DATE NOT NULL,
    date_fin       DATE,
    motif          VARCHAR(255),
    statut         VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
);

-- ---------------------------------------------------------------------
-- CONTRAINTES METIER (statuts)
-- ---------------------------------------------------------------------
ALTER TABLE mrtovo.poste DROP CONSTRAINT IF EXISTS ck_poste_statut;
ALTER TABLE mrtovo.poste ADD CONSTRAINT ck_poste_statut CHECK (statut IN ('ACTIF','INACTIF'));

ALTER TABLE mrtovo.offre_emploi DROP CONSTRAINT IF EXISTS ck_offre_emploi_statut;
ALTER TABLE mrtovo.offre_emploi ADD CONSTRAINT ck_offre_emploi_statut
    CHECK (statut IN ('BROUILLON','PUBLIEE','CLOTUREE','ARCHIVEE'));

ALTER TABLE mrtovo.candidature DROP CONSTRAINT IF EXISTS ck_candidature_statut;
ALTER TABLE mrtovo.candidature ADD CONSTRAINT ck_candidature_statut
    CHECK (statut IN ('EN_ATTENTE','EN_COURS_EVALUATION','EN_ENTRETIEN','RETENU',
                       'PROPOSITION_ENVOYEE','EN_NEGOCIATION','EMBAUCHE','REFUSE','ABANDONNE'));

ALTER TABLE mrtovo.proposition_offre DROP CONSTRAINT IF EXISTS ck_proposition_offre_statut;
ALTER TABLE mrtovo.proposition_offre ADD CONSTRAINT ck_proposition_offre_statut
    CHECK (statut IN ('BROUILLON','VALIDEE','EMISE','EN_NEGOCIATION','REVISE',
                       'ACCEPTEE','REFUSEE','EXPIREE','ANNULEE','SUPERSEDEE'));

ALTER TABLE mrtovo.proposition_offre DROP CONSTRAINT IF EXISTS ck_proposition_offre_type;
ALTER TABLE mrtovo.proposition_offre ADD CONSTRAINT ck_proposition_offre_type
    CHECK (type_contrat IN ('CDI','CDD','PERIODE_DESSAI','STAGE','ALTERNANCE'));

ALTER TABLE mrtovo.proposition_offre DROP CONSTRAINT IF EXISTS ck_proposition_offre_salaire;
ALTER TABLE mrtovo.proposition_offre ADD CONSTRAINT ck_proposition_offre_salaire CHECK (salaire_base >= 0);

ALTER TABLE mrtovo.proposition_offre DROP CONSTRAINT IF EXISTS ck_proposition_offre_periode;
ALTER TABLE mrtovo.proposition_offre ADD CONSTRAINT ck_proposition_offre_periode CHECK (periode_essai_mois >= 0);

ALTER TABLE mrtovo.avantage_offre DROP CONSTRAINT IF EXISTS ck_avantage_offre_type;
ALTER TABLE mrtovo.avantage_offre ADD CONSTRAINT ck_avantage_offre_type
    CHECK (type IN ('MUTUELLE','EQUIPEMENT','TELEPHONE','TRANSPORT','PRIMES',
                    'FORMATION','CONGE_SUPPLEMENTAIRE','AUTRE'));

ALTER TABLE mrtovo.negociation DROP CONSTRAINT IF EXISTS ck_negociation_statut;
ALTER TABLE mrtovo.negociation ADD CONSTRAINT ck_negociation_statut
    CHECK (statut IN ('EN_ATTENTE','EN_ANALYSE','ACCEPTE','REFUSE','CONTRE_PROPOSITION'));

ALTER TABLE mrtovo.negociation DROP CONSTRAINT IF EXISTS ck_negociation_type;
ALTER TABLE mrtovo.negociation ADD CONSTRAINT ck_negociation_type
    CHECK (type IN ('REVENDICATION','CONTRE_PROPOSITION','ACCORD_PARTIEL'));

ALTER TABLE mrtovo.employe DROP CONSTRAINT IF EXISTS ck_employe_statut;
ALTER TABLE mrtovo.employe ADD CONSTRAINT ck_employe_statut
    CHECK (statut IN ('EN_PERIODE_DESSAI','ACTIF','SUSPENDU','SORTI'));

ALTER TABLE mrtovo.contrat DROP CONSTRAINT IF EXISTS ck_contrat_statut;
ALTER TABLE mrtovo.contrat ADD CONSTRAINT ck_contrat_statut
    CHECK (statut_contrat IN ('A_SIGNER','SIGNE','EN_PERIODE_DESSAI','ACTIF','TERMINE','RUPTURE'));

ALTER TABLE mrtovo.contrat DROP CONSTRAINT IF EXISTS ck_contrat_type;
ALTER TABLE mrtovo.contrat ADD CONSTRAINT ck_contrat_type
    CHECK (type_contrat IN ('CDI','CDD','PERIODE_DESSAI','STAGE','ALTERNANCE'));

ALTER TABLE mrtovo.affectation DROP CONSTRAINT IF EXISTS ck_affectation_type;
ALTER TABLE mrtovo.affectation ADD CONSTRAINT ck_affectation_type
    CHECK (type IN ('INITIALE','MOBILITE','PROMOTION','TEMPORAIRE','RENOUVELLEMENT'));

ALTER TABLE mrtovo.affectation DROP CONSTRAINT IF EXISTS ck_affectation_statut;
ALTER TABLE mrtovo.affectation ADD CONSTRAINT ck_affectation_statut
    CHECK (statut IN ('ACTIVE','TERMINE','ANNULE'));

-- ---------------------------------------------------------------------
-- SEQUENCES METIER (matricule et reference de contrat)
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS mrtovo.seq_matricule START 1;
CREATE SEQUENCE IF NOT EXISTS mrtovo.seq_contrat_reference START 1;

