-- Exécuté à chaque démarrage : ne crée que ce qui manque, les données existantes sont conservées
-- (les offres sont créées depuis les demandes du Bloc 1 et doivent survivre aux redémarrages).
-- Pour repartir de zéro : DROP SCHEMA mrtovo CASCADE;
CREATE SCHEMA IF NOT EXISTS mrtovo;
SET search_path TO mrtovo;

-- ==========================================
-- 1. TABLES DE STATUTS
-- ==========================================
CREATE TABLE IF NOT EXISTS statut_candidature (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS statut_offre_emploi (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS statut_evaluation (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS statut_entretien (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);

-- ==========================================
-- 2. TABLES PRINCIPALES
-- ==========================================
CREATE TABLE IF NOT EXISTS candidat (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(50),
    fichier_cv_url VARCHAR(250)
);

CREATE TABLE IF NOT EXISTS offre_emploi (
    id SERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    date_publication DATE NOT NULL,
    statut_offre_emploi_id INT REFERENCES statut_offre_emploi(id)
);

-- Lien avec la demande de recrutement (public.demande_recrutement) : une offre par demande.
-- La clé étrangère est ajoutée par Hibernate, la table du Bloc 1 n'existant pas encore au premier démarrage.
ALTER TABLE offre_emploi ADD COLUMN IF NOT EXISTS id_demande BIGINT UNIQUE;

-- ==========================================
-- 3. TABLES DÉPENDANTES
-- ==========================================
CREATE TABLE IF NOT EXISTS candidature (
    id SERIAL PRIMARY KEY,
    candidat_id INT NOT NULL REFERENCES candidat(id) ON DELETE CASCADE,
    offre_emploi_id INT NOT NULL REFERENCES offre_emploi(id) ON DELETE CASCADE,
    date_candidature DATE NOT NULL,
    statut_candidature_id INT REFERENCES statut_candidature(id),
    UNIQUE (candidat_id, offre_emploi_id)
);

-- Test : une évaluation par candidature, créée lors de la présélection
CREATE TABLE IF NOT EXISTS evaluation (
    id SERIAL PRIMARY KEY,
    candidature_id INT NOT NULL UNIQUE REFERENCES candidature(id) ON DELETE CASCADE,
    note_test NUMERIC(4,2) CHECK (note_test BETWEEN 0 AND 20),
    statut_evaluation_id INT REFERENCES statut_evaluation(id)
);

-- Entretien : un ou plusieurs (si annulé puis replanifié) par évaluation
CREATE TABLE IF NOT EXISTS entretien (
    id SERIAL PRIMARY KEY,
    evaluation_id INT NOT NULL REFERENCES evaluation(id) ON DELETE CASCADE,
    date_entretien TIMESTAMP NOT NULL,
    lieu VARCHAR(255),
    commentaire TEXT,
    statut_entretien_id INT REFERENCES statut_entretien(id)
);

-- Historique d'une candidature : une ligne par action, avec son auteur et sa date. Jamais modifiée.
CREATE TABLE IF NOT EXISTS historique_candidature (
    id SERIAL PRIMARY KEY,
    candidature_id INT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    action VARCHAR(40) NOT NULL,
    ancien_statut_id INT REFERENCES statut_candidature(id),
    nouveau_statut_id INT REFERENCES statut_candidature(id),
    auteur VARCHAR(255) NOT NULL,
    details TEXT,
    date_action TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_historique_candidature ON historique_candidature(candidature_id, date_action);

ALTER TABLE historique_candidature DROP CONSTRAINT IF EXISTS ck_historique_candidature_action;
ALTER TABLE historique_candidature ADD CONSTRAINT ck_historique_candidature_action
    CHECK (action IN ('CANDIDATURE_DEPOSEE','PRESELECTION_RETENUE','PRESELECTION_REJETEE','NOTE_SAISIE',
                      'NOTE_CORRIGEE','TEST_VALIDE','TEST_ECHOUE','ENTRETIEN_PLANIFIE','ENTRETIEN_ANNULE',
                      'CANDIDAT_ACCEPTE','CANDIDAT_REFUSE'));

-- Une ligne d'historique ne se corrige pas : toute modification est ignorée par la base.
CREATE OR REPLACE RULE historique_candidature_non_modifiable AS
    ON UPDATE TO historique_candidature DO INSTEAD NOTHING;

-- Remet le search_path par défaut : la connexion retourne au pool et sert ensuite au Bloc 1 (schéma public).
RESET search_path;

-- Compatibilite avec les anciennes tables creees par le prototype du Bloc 3.
-- Les blocs 1 et 2 utilisent les tables de reference de statut.
ALTER TABLE mrtovo.offre_emploi
    ADD COLUMN IF NOT EXISTS statut_offre_emploi_id INT;
ALTER TABLE mrtovo.offre_emploi
    ADD COLUMN IF NOT EXISTS statut VARCHAR(30);
UPDATE mrtovo.offre_emploi
SET statut_offre_emploi_id = CASE UPPER(statut)
    WHEN 'PUBLIEE' THEN 1
    WHEN 'CLOTUREE' THEN 2
    ELSE 3
END
WHERE statut_offre_emploi_id IS NULL;

ALTER TABLE mrtovo.candidature
    ADD COLUMN IF NOT EXISTS statut_candidature_id INT;
ALTER TABLE mrtovo.candidature
    ADD COLUMN IF NOT EXISTS statut VARCHAR(30);
UPDATE mrtovo.candidature
SET statut_candidature_id = CASE UPPER(statut)
    WHEN 'RETENU' THEN 4
    WHEN 'REFUSE' THEN 5
    WHEN 'EN_COURS_EVALUATION' THEN 2
    WHEN 'EN_ENTRETIEN' THEN 3
    ELSE 1
END
WHERE statut_candidature_id IS NULL;

-- L'offre du Bloc 2 référence la demande du Bloc 1, stockée dans public.
-- Supprime une éventuelle ancienne FK créée avant la séparation des schémas.
ALTER TABLE mrtovo.offre_emploi
    DROP CONSTRAINT IF EXISTS fkqh5a2e4buyr8fvovo3vx0c6w1;

ALTER TABLE mrtovo.evaluation
    ADD COLUMN IF NOT EXISTS statut_evaluation_id INT;
ALTER TABLE mrtovo.entretien
    ADD COLUMN IF NOT EXISTS statut_entretien_id INT;
ALTER TABLE mrtovo.entretien
    ADD COLUMN IF NOT EXISTS date_entretien TIMESTAMP;
ALTER TABLE mrtovo.entretien
    ADD COLUMN IF NOT EXISTS lieu VARCHAR(255);
ALTER TABLE mrtovo.entretien
    ADD COLUMN IF NOT EXISTS commentaire TEXT;

-- Les versions précédentes plaçaient aussi les entités Bloc 1 dans mrtovo.
-- Leurs colonnes d'identifiant restent présentes dans ces tables ; elles ne
-- doivent plus empêcher les insertions Bloc 3, qui utilisent la colonne id.
ALTER TABLE IF EXISTS mrtovo.departement
    ADD COLUMN IF NOT EXISTS id_departement BIGINT;
ALTER TABLE IF EXISTS mrtovo.departement
    ALTER COLUMN id_departement DROP IDENTITY IF EXISTS;
ALTER TABLE IF EXISTS mrtovo.departement
    ALTER COLUMN id_departement DROP NOT NULL;
ALTER TABLE IF EXISTS mrtovo.poste
    ADD COLUMN IF NOT EXISTS id_poste BIGINT;
ALTER TABLE IF EXISTS mrtovo.poste
    ALTER COLUMN id_poste DROP IDENTITY IF EXISTS;
ALTER TABLE IF EXISTS mrtovo.poste
    ALTER COLUMN id_poste DROP NOT NULL;
ALTER TABLE IF EXISTS mrtovo.grille_salariale
    ADD COLUMN IF NOT EXISTS id_grille BIGINT;
ALTER TABLE IF EXISTS mrtovo.grille_salariale
    ALTER COLUMN id_grille DROP IDENTITY IF EXISTS;
ALTER TABLE IF EXISTS mrtovo.grille_salariale
    ALTER COLUMN id_grille DROP NOT NULL;
ALTER TABLE IF EXISTS mrtovo.employe
    ADD COLUMN IF NOT EXISTS id_employe BIGINT;
ALTER TABLE IF EXISTS mrtovo.employe
    ALTER COLUMN id_employe DROP IDENTITY IF EXISTS;
ALTER TABLE IF EXISTS mrtovo.employe
    ALTER COLUMN id_employe DROP NOT NULL;

-- ==========================================
-- 4. REFERENTIELS ET PARCOURS D'EMBAUCHE
-- ==========================================
CREATE TABLE IF NOT EXISTS mrtovo.departement (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(50) UNIQUE
);

CREATE TABLE IF NOT EXISTS mrtovo.poste (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    departement_id BIGINT REFERENCES mrtovo.departement(id),
    statut VARCHAR(30) NOT NULL DEFAULT 'ACTIF'
);

CREATE TABLE IF NOT EXISTS mrtovo.grille_salariale (
    id BIGSERIAL PRIMARY KEY,
    poste_id BIGINT NOT NULL REFERENCES mrtovo.poste(id),
    niveau VARCHAR(50) NOT NULL,
    salaire_min NUMERIC(12,2) NOT NULL,
    salaire_max NUMERIC(12,2) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE,
    CONSTRAINT uq_grille_salariale UNIQUE (poste_id, niveau, date_debut),
    CONSTRAINT ck_grille_salariale_bornes CHECK (salaire_min <= salaire_max)
);

CREATE TABLE IF NOT EXISTS mrtovo.proposition_offre (
    id BIGSERIAL PRIMARY KEY,
    candidature_id BIGINT NOT NULL REFERENCES mrtovo.candidature(id),
    version INTEGER NOT NULL DEFAULT 1,
    proposition_parent_id BIGINT REFERENCES mrtovo.proposition_offre(id),
    salaire_base NUMERIC(12,2) NOT NULL,
    temps_travail VARCHAR(100) NOT NULL,
    emploi_du_temps VARCHAR(255),
    lieu_travail VARCHAR(255) NOT NULL,
    date_prise_poste DATE NOT NULL,
    date_limite_validite DATE NOT NULL,
    date_emission DATE NOT NULL,
    date_fin_contrat DATE,
    type_contrat VARCHAR(30) NOT NULL DEFAULT 'CDI',
    periode_essai_mois INTEGER NOT NULL DEFAULT 0,
    poste_id BIGINT REFERENCES mrtovo.poste(id),
    departement_id BIGINT REFERENCES mrtovo.departement(id),
    statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
    motif_revision VARCHAR(255),
    validee_par VARCHAR(255),
    date_validation DATE,
    date_acceptation DATE,
    CONSTRAINT uq_proposition_offre_version UNIQUE (candidature_id, version)
);

ALTER TABLE mrtovo.proposition_offre
    ADD COLUMN IF NOT EXISTS date_fin_contrat DATE;

CREATE TABLE IF NOT EXISTS mrtovo.avantage_offre (
    id BIGSERIAL PRIMARY KEY,
    proposition_id BIGINT NOT NULL REFERENCES mrtovo.proposition_offre(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    montant NUMERIC(12,2),
    ordre INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mrtovo.negociation (
    id BIGSERIAL PRIMARY KEY,
    proposition_id BIGINT NOT NULL REFERENCES mrtovo.proposition_offre(id),
    candidat_id BIGINT NOT NULL REFERENCES mrtovo.candidat(id),
    type VARCHAR(50) NOT NULL DEFAULT 'REVENDICATION',
    salaire_revendique NUMERIC(12,2),
    avantage_revendique VARCHAR(255),
    motif VARCHAR(255) NOT NULL,
    date_demande DATE NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    decide_par VARCHAR(255),
    date_decision DATE,
    commentaire_decision VARCHAR(255),
    proposition_resultante_id BIGINT REFERENCES mrtovo.proposition_offre(id)
);

CREATE TABLE IF NOT EXISTS mrtovo.historique_proposition (
    id BIGSERIAL PRIMARY KEY,
    proposition_id BIGINT NOT NULL REFERENCES mrtovo.proposition_offre(id) ON DELETE CASCADE,
    ancien_statut VARCHAR(30),
    nouveau_statut VARCHAR(30) NOT NULL,
    action VARCHAR(50) NOT NULL,
    auteur VARCHAR(255),
    commentaire VARCHAR(255),
    date_changement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mrtovo.historique_negociation (
    id BIGSERIAL PRIMARY KEY,
    negociation_id BIGINT NOT NULL REFERENCES mrtovo.negociation(id) ON DELETE CASCADE,
    ancien_statut VARCHAR(30),
    nouveau_statut VARCHAR(30) NOT NULL,
    action VARCHAR(50) NOT NULL,
    auteur VARCHAR(255),
    commentaire VARCHAR(255),
    date_changement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mrtovo.employe (
    id BIGSERIAL PRIMARY KEY,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    candidat_id BIGINT NOT NULL UNIQUE REFERENCES mrtovo.candidat(id),
    proposition_id BIGINT NOT NULL UNIQUE REFERENCES mrtovo.proposition_offre(id),
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(50),
    adresse VARCHAR(255),
    poste_id BIGINT REFERENCES mrtovo.poste(id),
    departement_id BIGINT REFERENCES mrtovo.departement(id),
    date_embauche DATE NOT NULL,
    date_entree DATE NOT NULL,
    date_fin_contrat DATE,
    date_fin_periode_essai DATE,
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_PERIODE_DESSAI'
);

CREATE TABLE IF NOT EXISTS mrtovo.contrat (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES mrtovo.employe(id),
    reference VARCHAR(50) NOT NULL UNIQUE,
    type_contrat VARCHAR(30) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE,
    salaire NUMERIC(12,2) NOT NULL,
    statut_contrat VARCHAR(30) NOT NULL DEFAULT 'A_SIGNER',
    document_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS mrtovo.affectation (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES mrtovo.employe(id),
    poste_id BIGINT NOT NULL REFERENCES mrtovo.poste(id),
    departement_id BIGINT REFERENCES mrtovo.departement(id),
    type VARCHAR(50) NOT NULL DEFAULT 'INITIALE',
    date_debut DATE NOT NULL,
    date_fin DATE,
    motif VARCHAR(255),
    statut VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
);

-- Contraintes de domaine re-appliquees pour les bases deja initialisees.
ALTER TABLE mrtovo.poste DROP CONSTRAINT IF EXISTS ck_poste_statut;
ALTER TABLE mrtovo.poste ADD CONSTRAINT ck_poste_statut
    CHECK (statut IN ('ACTIF','INACTIF'));

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

CREATE SEQUENCE IF NOT EXISTS mrtovo.seq_matricule START 1;
CREATE SEQUENCE IF NOT EXISTS mrtovo.seq_contrat_reference START 1;
