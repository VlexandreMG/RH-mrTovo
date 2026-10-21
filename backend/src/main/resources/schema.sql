CREATE SCHEMA IF NOT EXISTS mrtovo;

-- Tonga dia tadiavina ilay schéma 
SET search_path TO mrtovo;

CREATE TABLE candidat (
    id SERIALIZE PRIMARY KEY,
    nom VARHCHAR(255),
    prenom VARHCHAR(255),
    email VARHCHAR(255),
    telephone VARCHAR(50),
    fichierCVUrl VARCHAR(250)
);