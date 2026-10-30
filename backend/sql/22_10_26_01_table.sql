-- Table provisoire pour l'employée 
CREATE TABLE Employee(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telephone VARCHAR(20) NOT NULL,
    adresse VARCHAR(200) NOT NULL
);
-- Pour voir si c'est un CDD,CDI ou Stage
CREATE TABLE TypeContrat(
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(50) NOT NULL
);
 
CREATE TABLE StatutContrat(
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(50) NOT NULL
);

CREATE TABLE Contrat(
    id SERIAL PRIMARY KEY ,
    DateDebut DATE NOT NULL,
    DateFin DATE NOT NULL,
    idTypeContrat INT NOT NULL,
    salaire DECIMAL(12,2) NOT NULL,
    idStatutContrat INT NOT NULL,
    idEmployee INT NOT NULL,
    FOREIGN KEY (idTypeContrat) REFERENCES TypeContrat(id),
    FOREIGN KEY (idStatutContrat) REFERENCES StatutContrat(id),
    FOREIGN KEY (idEmployee) REFERENCES Employee(id)

);

