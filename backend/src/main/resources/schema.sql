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

