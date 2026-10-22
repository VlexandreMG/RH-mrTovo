CREATE SCHEMA IF NOT EXISTS mrtovo;
SET search_path TO mrtovo;

-- ==========================================
-- 1. TABLES DE STATUTS (Créées en premier)
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
-- 2. TABLES PRINCIPALES (Mères)
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
-- 3. TABLES Dépendantes (Enfants)
-- ==========================================
CREATE TABLE candidature (
    id SERIAL PRIMARY KEY,
    candidat_id INT REFERENCES candidat(id) ON DELETE CASCADE,
    offre_emploi_id INT REFERENCES offre_emploi(id) ON DELETE CASCADE,
    date_candidature DATE NOT NULL,
    statut_candidature_id INT REFERENCES statut_candidature(id)
);

CREATE TABLE evaluation (
    id SERIAL PRIMARY KEY,
    candidature_id INT REFERENCES candidature(id) ON DELETE CASCADE,
    note_test INT,
    statut_evaluation_id INT REFERENCES statut_evaluation(id)
);

CREATE TABLE entretien (
    id SERIAL PRIMARY KEY,
    evaluation_id INT REFERENCES evaluation(id) ON DELETE CASCADE,
    date_entretien DATE NOT NULL,
    statut_entretien_id INT REFERENCES statut_entretien(id) 
);