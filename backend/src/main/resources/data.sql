-- Données de démonstration : insérées une seule fois (ON CONFLICT DO NOTHING), jamais écrasées.
SET search_path TO mrtovo;

-- ==========================================
-- 1. STATUTS (les ID sont utilisés dans le code : ne pas les modifier)
-- ==========================================
INSERT INTO statut_candidature (id, libelle) VALUES
(1, 'À traiter'),
(2, 'Test'),
(3, 'Attente entretien'),
(4, 'Accepté'),
(5, 'Refusé') ON CONFLICT DO NOTHING;

INSERT INTO statut_offre_emploi (id, libelle) VALUES
(1, 'Ouverte'),
(2, 'Fermée'),
(3, 'En attente') ON CONFLICT DO NOTHING;  -- offre créée avec la demande, publiée à l'ouverture du recrutement

INSERT INTO statut_evaluation (id, libelle) VALUES
(1, 'En attente'),
(2, 'Validé'),
(3, 'Échoué') ON CONFLICT DO NOTHING;

INSERT INTO statut_entretien (id, libelle) VALUES
(1, 'Planifié'),
(2, 'Terminé'),
(3, 'Annulé') ON CONFLICT DO NOTHING;

SELECT setval('statut_candidature_id_seq', (SELECT MAX(id) FROM statut_candidature));
SELECT setval('statut_offre_emploi_id_seq', (SELECT MAX(id) FROM statut_offre_emploi));
SELECT setval('statut_evaluation_id_seq', (SELECT MAX(id) FROM statut_evaluation));
SELECT setval('statut_entretien_id_seq', (SELECT MAX(id) FROM statut_entretien));

-- Referentiels de recrutement partages avec les ecrans de propositions.
INSERT INTO departement (nom, code)
VALUES ('Informatique', 'INFO')
ON CONFLICT DO NOTHING;

INSERT INTO poste (titre, description, departement_id, statut)
SELECT 'Developpeur Java / Spring Boot',
       'Developpement backend Java / Spring Boot',
       departement.id,
       'ACTIF'
FROM departement
WHERE departement.code = 'INFO'
  AND NOT EXISTS (
      SELECT 1
      FROM poste
      WHERE poste.titre = 'Developpeur Java / Spring Boot'
  );

INSERT INTO grille_salariale (poste_id, niveau, salaire_min, salaire_max, date_debut)
SELECT poste.id, 'CONFIRME', 3000000, 4500000, DATE '2025-01-01'
FROM poste
WHERE poste.titre = 'Developpeur Java / Spring Boot'
ON CONFLICT DO NOTHING;

INSERT INTO grille_salariale (poste_id, niveau, salaire_min, salaire_max, date_debut)
SELECT poste.id, 'SENIOR', 4500000, 6000000, DATE '2025-01-01'
FROM poste
WHERE poste.titre = 'Developpeur Java / Spring Boot'
ON CONFLICT DO NOTHING;

-- ==========================================
-- 2. OFFRE D'EMPLOI
-- ==========================================
INSERT INTO offre_emploi (id, titre, description, date_publication, statut_offre_emploi_id) VALUES
(1, 'Développeur Fullstack Java / Vue.js', 'Poste de développeur pour la conception et le suivi de systèmes RH.', CURRENT_DATE, 1) ON CONFLICT DO NOTHING;

SELECT setval('offre_emploi_id_seq', (SELECT MAX(id) FROM offre_emploi));

-- ==========================================
-- 3. CANDIDATS
-- ==========================================
INSERT INTO candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES
(1, 'Rakoto', 'Jean', 'jean.rakoto@gmail.com', '0340000001', '/cv/jean.pdf'),
(2, 'Rabe', 'Marie', 'marie.rabe@gmail.com', '0340000002', '/cv/marie.pdf'),
(3, 'Andria', 'Luc', 'luc.andria@gmail.com', '0340000003', '/cv/luc.pdf'),
(4, 'Rasoa', 'Sarah', 'sarah.rasoa@gmail.com', '0340000004', '/cv/sarah.pdf'),
(5, 'Randria', 'Kevin', 'kevin.randria@gmail.com', '0340000005', '/cv/kevin.pdf'),
(6, 'Ranaivo', 'Aina', 'aina.ranaivo@gmail.com', '0340000006', '/cv/aina.pdf'),
(7, 'Razafy', 'Paul', 'paul.razafy@gmail.com', '0340000007', '/cv/paul.pdf') ON CONFLICT DO NOTHING;

SELECT setval('candidat_id_seq', (SELECT MAX(id) FROM candidat));

-- ==========================================
-- 4. CANDIDATURES (offre #1), une par étape du parcours
-- ==========================================
INSERT INTO candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id) VALUES
(1, 1, 1, CURRENT_DATE - INTERVAL '5 days', 1),  -- À traiter
(2, 2, 1, CURRENT_DATE - INTERVAL '4 days', 1),  -- À traiter
(3, 3, 1, CURRENT_DATE - INTERVAL '3 days', 2),  -- Test : note pas encore saisie
(4, 4, 1, CURRENT_DATE - INTERVAL '3 days', 2),  -- Test : note saisie, décision à prendre
(5, 5, 1, CURRENT_DATE - INTERVAL '2 days', 3),  -- Attente entretien (entretien planifié)
(6, 6, 1, CURRENT_DATE - INTERVAL '1 day', 4),   -- Accepté (→ Sarobidy)
(7, 7, 1, CURRENT_DATE - INTERVAL '6 days', 5) ON CONFLICT DO NOTHING;  -- Refusé à la présélection

SELECT setval('candidature_id_seq', (SELECT MAX(id) FROM candidature));

-- ==========================================
-- 5. ÉVALUATIONS (tests)
-- ==========================================
INSERT INTO evaluation (id, candidature_id, note_test, statut_evaluation_id) VALUES
(1, 3, NULL, 1),
(2, 4, 14.5, 1),
(3, 5, 16, 2),
(4, 6, 17, 2) ON CONFLICT DO NOTHING;

SELECT setval('evaluation_id_seq', (SELECT MAX(id) FROM evaluation));

-- ==========================================
-- 6. ENTRETIENS
-- ==========================================
INSERT INTO entretien (id, evaluation_id, date_entretien, lieu, commentaire, statut_entretien_id) VALUES
(1, 3, CURRENT_DATE + INTERVAL '2 days 10 hours', 'Salle de réunion 1', NULL, 1),
(2, 4, CURRENT_DATE - INTERVAL '1 day' + INTERVAL '14 hours', 'Salle de réunion 1', 'Très bon profil, motivée.', 2) ON CONFLICT DO NOTHING;

SELECT setval('entretien_id_seq', (SELECT MAX(id) FROM entretien));

-- ==========================================
-- 7. BLOC 6 : REGIMES DE TRAVAIL
-- ==========================================
INSERT INTO regime_travail (id, code, libelle, tolerance_retard_minutes, cree_par) VALUES
(1, 'BUREAU_40H', 'Bureau 40 h (lundi au vendredi)', 10, 'systeme'),
(2, 'SERVICE_SAMEDI_40H', 'Service 40 h (samedi matin inclus)', 10, 'systeme') ON CONFLICT DO NOTHING;

SELECT setval('regime_travail_id_seq', (SELECT MAX(id) FROM regime_travail));

INSERT INTO horaire_regime (regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES
(1, 1, '08:00', '17:00', 60), (1, 2, '08:00', '17:00', 60), (1, 3, '08:00', '17:00', 60),
(1, 4, '08:00', '17:00', 60), (1, 5, '08:00', '17:00', 60),
(2, 1, '08:00', '16:00', 60), (2, 2, '08:00', '16:00', 60), (2, 3, '08:00', '16:00', 60),
(2, 4, '08:00', '16:00', 60), (2, 5, '08:00', '16:00', 60), (2, 6, '08:00', '13:00', 0)
ON CONFLICT DO NOTHING;

-- Remet le search_path par défaut : la connexion retourne au pool et sert ensuite au Bloc 1 (schéma public).
RESET search_path;
