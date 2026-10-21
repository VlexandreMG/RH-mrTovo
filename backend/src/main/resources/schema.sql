CREATE SCHEMA IF NOT EXISTS mrtovo;

-- Tonga dia tadiavina ilay schéma 
SET search_path TO mrtovo;

CREATE TABLE candidat (
    id SERIALIZE PRIMARY KEY,
    nom VARHCHAR(255) NOT NULL,
    prenom VARHCHAR(255) NOT NULL,
    email VARHCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(50),
    fichierCVUrl VARCHAR(250)
);