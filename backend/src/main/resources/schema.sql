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

-- Remet le search_path par défaut : la connexion retourne au pool et sert ensuite au Bloc 1 (schéma public).
RESET search_path;
