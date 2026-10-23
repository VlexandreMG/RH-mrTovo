-- ATTENTION (dev) : le schéma est recréé à chaque démarrage de l'application.
DROP SCHEMA IF EXISTS mrtovo CASCADE;
CREATE SCHEMA mrtovo;
SET search_path TO mrtovo;

-- ==========================================
-- 1. TABLES DE STATUTS
-- ==========================================
CREATE TABLE statut_candidature (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);

CREATE TABLE statut_offre_emploi (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);

CREATE TABLE statut_evaluation (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);

CREATE TABLE statut_entretien (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);

-- ==========================================
-- 2. TABLES PRINCIPALES
-- ==========================================
CREATE TABLE candidat (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(50),
    fichier_cv_url VARCHAR(250)
);

CREATE TABLE offre_emploi (
    id SERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    date_publication DATE NOT NULL,
    statut_offre_emploi_id INT REFERENCES statut_offre_emploi(id)
);

-- ==========================================
-- 3. TABLES DÉPENDANTES
-- ==========================================
CREATE TABLE candidature (
    id SERIAL PRIMARY KEY,
    candidat_id INT NOT NULL REFERENCES candidat(id) ON DELETE CASCADE,
    offre_emploi_id INT NOT NULL REFERENCES offre_emploi(id) ON DELETE CASCADE,
    date_candidature DATE NOT NULL,
    statut_candidature_id INT REFERENCES statut_candidature(id),
    UNIQUE (candidat_id, offre_emploi_id)
);

-- Test : une évaluation par candidature, créée lors de la présélection
CREATE TABLE evaluation (
    id SERIAL PRIMARY KEY,
    candidature_id INT NOT NULL UNIQUE REFERENCES candidature(id) ON DELETE CASCADE,
    note_test NUMERIC(4,2) CHECK (note_test BETWEEN 0 AND 20),
    statut_evaluation_id INT REFERENCES statut_evaluation(id)
);

-- Entretien : un ou plusieurs (si annulé puis replanifié) par évaluation
CREATE TABLE entretien (
    id SERIAL PRIMARY KEY,
    evaluation_id INT NOT NULL REFERENCES evaluation(id) ON DELETE CASCADE,
    date_entretien TIMESTAMP NOT NULL,
    lieu VARCHAR(255),
    commentaire TEXT,
    statut_entretien_id INT REFERENCES statut_entretien(id)
);

-- Remet le search_path par défaut : la connexion retourne au pool et sert ensuite au Bloc 1 (schéma public).
RESET search_path;
