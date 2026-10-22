CREATE SCHEMA IF NOT EXISTS mrtovo;

-- Tonga dia tadiavina ilay schéma 
SET search_path TO mrtovo;

CREATE TABLE Candidat (
    id SERIALIZE PRIMARY KEY,
    nom VARHCHAR(255) NOT NULL,
    prenom VARHCHAR(255) NOT NULL,
    email VARHCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(50),
    fichierCVUrl VARCHAR(250)
);

CREATE TABLE Candidature (
    id SERIALIZE PRIMARY KEY,
    candidat_id NUMBER FOREIGN KEY,
    offreEmploi_id NUMBER FOREIGN KEY,
    dateCandidature DATE NOT NULL,
    statut ENUM
);

CREATE TABLE OffreEmploi (
    id SERIALIZE PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    datePublication DATE NOT NULL,
    statut ENUM 
);

CREATE TABLE Evaluation (
    id SERIALIZE PRIMARY KEY,
    candidatue_id NUMBER FOREIGN KEY,
    noteTest NUMBER,
    statut ENUM
);

CREATE TABLE Entretien (
    id SERIALIZE PRIMARY KEY,
    evaluation_id NUMBER FOREIGN KEY,
    dateEntretien DATE NOT NULL,
    statut ENUM 
);

CREATE TABLE StatutCandidature (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);

CREATE TABLE StatutOffreEmploi (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL
);