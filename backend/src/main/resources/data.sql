SET search_path TO mrtovo;

-- ==========================================
-- 1. INSERTION DES STATUTS
-- ==========================================
INSERT INTO statut_candidature (id, libelle) VALUES
(1, 'À traiter'),
(2, 'Présélectionné'),
(3, 'Entretien'),
(4, 'Accepté'),
(5, 'Refusé');

INSERT INTO statut_offre_emploi (id, libelle) VALUES
(1, 'Ouverte'),
(2, 'Fermée');

INSERT INTO statut_evaluation (id, libelle) VALUES
(1, 'En attente'),
(2, 'Validé'),
(3, 'Échoué');

INSERT INTO statut_entretien (id, libelle) VALUES
(1, 'Planifié'),
(2, 'Terminé'),
(3, 'Annulé');

-- Synchronisation des séquences d'ID pour les statuts
SELECT setval('statut_candidature_id_seq', (SELECT MAX(id) FROM statut_candidature));
SELECT setval('statut_offre_emploi_id_seq', (SELECT MAX(id) FROM statut_offre_emploi));
SELECT setval('statut_evaluation_id_seq', (SELECT MAX(id) FROM statut_evaluation));
SELECT setval('statut_entretien_id_seq', (SELECT MAX(id) FROM statut_entretien));

-- ==========================================
-- 2. INSERTION DE L'OFFRE D'EMPLOI (#1)
-- ==========================================
INSERT INTO offre_emploi (id, titre, description, date_publication, statut_offre_emploi_id) VALUES
(1, 'Développeur Fullstack Java / Vue.js', 'Poste de développeur pour la conception et le suivi de systèmes RH.', CURRENT_DATE, 1);

SELECT setval('offre_emploi_id_seq', (SELECT MAX(id) FROM offre_emploi));

-- ==========================================
-- 3. INSERTION DES CANDIDATS
-- ==========================================
INSERT INTO candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES
(1, 'Rakoto', 'Jean', 'jean.rakoto@email.com', '0340000001', '/cv/jean.pdf'),
(2, 'Rabe', 'Marie', 'marie.rabe@email.com', '0340000002', '/cv/marie.pdf'),
(3, 'Andria', 'Luc', 'luc.andria@email.com', '0340000003', '/cv/luc.pdf'),
(4, 'Rasoa', 'Sarah', 'sarah.rasoa@email.com', '0340000004', '/cv/sarah.pdf'),
(5, 'Randria', 'Kevin', 'kevin.randria@email.com', '0340000005', '/cv/kevin.pdf'),
(6, 'Ranaivo', 'Aina', 'aina.ranaivo@email.com', '0340000006', '/cv/aina.pdf'),
(7, 'Razafy', 'Paul', 'paul.razafy@email.com', '0340000007', '/cv/paul.pdf');

SELECT setval('candidat_id_seq', (SELECT MAX(id) FROM candidat));

-- ==========================================
-- 4. INSERTION DES CANDIDATURES (Pour l'offre #1)
-- ==========================================
INSERT INTO candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id) VALUES
-- Colonne 1 : À traiter (statut 1)
(1, 1, 1, CURRENT_DATE - INTERVAL '5 days', 1),
(2, 2, 1, CURRENT_DATE - INTERVAL '4 days', 1),

-- Colonne 2 : Présélectionné (statut 2)
(3, 3, 1, CURRENT_DATE - INTERVAL '3 days', 2),
(4, 4, 1, CURRENT_DATE - INTERVAL '3 days', 2),

-- Colonne 3 : Entretien (statut 3)
(5, 5, 1, CURRENT_DATE - INTERVAL '2 days', 3),

-- Colonne 4 : Accepté (statut 4)
(6, 6, 1, CURRENT_DATE - INTERVAL '1 day', 4),

-- Colonne 5 : Refusé (statut 5)
(7, 7, 1, CURRENT_DATE - INTERVAL '6 days', 5);

SELECT setval('candidature_id_seq', (SELECT MAX(id) FROM candidature));