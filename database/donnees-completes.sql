-- =====================================================================
-- Donnees completes de la base rh_db (schemas public et mrtovo).
-- Export du 13/11/2026 : comptes, candidatures, tests, salaries, contrats,
-- pointages, conges, historique et journal d'audit.
--
-- A charger APRES un premier demarrage du backend (qui cree les tables) :
--   docker exec -i postgres-16 psql -U mrtovo -d rh_db < database/donnees-completes.sql
--
-- Rejouable : chaque INSERT ignore une ligne qui existe deja (ON CONFLICT DO NOTHING ; pour
-- historique_candidature, dont une regle interdit ON CONFLICT : WHERE NOT EXISTS).
-- Les fichiers deposes (CV, documents, justificatifs) ne sont pas dans la base :
-- ils restent dans les dossiers d'upload du backend.
-- Quatre anciennes colonnes, toujours vides (departement.id_departement, employe.id_employe,
-- grille_salariale.id_grille, poste.id_poste), sont omises : le schema actuel ne les a plus.
-- =====================================================================
--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14 (Debian 16.14-1.pgdg13+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- Plusieurs tables se referencent mutuellement (offre <-> demande, employe <-> utilisateur...) :
-- les cles etrangeres ne sont pas verifiees pendant le chargement, puis sont retablies a la fin.
SET session_replication_role = replica;

--
-- Data for Name: candidat; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES (1, 'Rakoto', 'Jean', 'jean.rakoto@gmail.com', '0340000001', '/cv/jean.pdf') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES (2, 'Rabe', 'Marie', 'marie.rabe@gmail.com', '0340000002', '/cv/marie.pdf') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES (3, 'Andria', 'Luc', 'luc.andria@gmail.com', '0340000003', '/cv/luc.pdf') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES (4, 'Rasoa', 'Sarah', 'sarah.rasoa@gmail.com', '0340000004', '/cv/sarah.pdf') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES (5, 'Randria', 'Kevin', 'kevin.randria@gmail.com', '0340000005', '/cv/kevin.pdf') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES (6, 'Ranaivo', 'Aina', 'aina.ranaivo@gmail.com', '0340000006', '/cv/aina.pdf') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES (7, 'Razafy', 'Paul', 'paul.razafy@gmail.com', '0340000007', '/cv/paul.pdf') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES (8, 'RAK', 'Alexandre', 'batmanhoodie@gmail.com', '0347407375', '90f00205-db98-4414-9e32-5322b37df472.pdf') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidat (id, nom, prenom, email, telephone, fichier_cv_url) VALUES (9, 'epifj', 'einf', 'huhu@gmail.com', 'oeifgn', 'b7d3877f-3175-4743-bb7d-2a4dd3f0d4da.pdf') ON CONFLICT DO NOTHING;


--
-- Data for Name: statut_offre_emploi; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.statut_offre_emploi (id, libelle) VALUES (1, 'Ouverte') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_offre_emploi (id, libelle) VALUES (2, 'Fermée') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_offre_emploi (id, libelle) VALUES (3, 'En attente') ON CONFLICT DO NOTHING;


--
-- Data for Name: offre_emploi; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.offre_emploi (id, titre, description, date_publication, statut_offre_emploi_id, id_demande, statut, date_cloture) VALUES (1, 'Développeur Fullstack Java / Vue.js', 'Poste de développeur pour la conception et le suivi de systèmes RH.', '2026-11-03', 1, NULL, NULL, NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: statut_candidature; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.statut_candidature (id, libelle) VALUES (1, 'À traiter') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_candidature (id, libelle) VALUES (2, 'Test') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_candidature (id, libelle) VALUES (3, 'Attente entretien') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_candidature (id, libelle) VALUES (4, 'Accepté') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_candidature (id, libelle) VALUES (5, 'Refusé') ON CONFLICT DO NOTHING;


--
-- Data for Name: candidature; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id, statut) VALUES (2, 2, 1, '2026-10-30', 1, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id, statut) VALUES (4, 4, 1, '2026-10-31', 2, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id, statut) VALUES (5, 5, 1, '2026-11-01', 3, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id, statut) VALUES (7, 7, 1, '2026-10-28', 5, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id, statut) VALUES (3, 3, 1, '2026-10-31', 2, 'EMBAUCHE') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id, statut) VALUES (6, 6, 1, '2026-11-02', 4, 'EMBAUCHE') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id, statut) VALUES (1, 1, 1, '2026-10-29', 4, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id, statut) VALUES (8, 8, 1, '2026-11-13', 5, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.candidature (id, candidat_id, offre_emploi_id, date_candidature, statut_candidature_id, statut) VALUES (9, 9, 1, '2026-11-13', 3, NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: departement; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.departement (id, nom, code) VALUES (1, 'Informatique', 'INFO') ON CONFLICT DO NOTHING;


--
-- Data for Name: poste; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.poste (id, titre, description, departement_id, statut) VALUES (1, 'Developpeur Java / Spring Boot', 'Developpement backend Java / Spring Boot', 1, 'ACTIF') ON CONFLICT DO NOTHING;


--
-- Data for Name: proposition_offre; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.proposition_offre (id, candidature_id, version, proposition_parent_id, salaire_base, temps_travail, emploi_du_temps, lieu_travail, date_prise_poste, date_limite_validite, date_emission, date_fin_contrat, type_contrat, periode_essai_mois, poste_id, departement_id, statut, motif_revision, validee_par, date_validation, date_acceptation) VALUES (4, 6, 1, NULL, 1000000.00, 'Temps plein', NULL, 'Antananarivo', '2025-03-03', '2025-02-03', '2025-02-03', NULL, 'CDI', 6, 1, 1, 'ACCEPTEE', NULL, NULL, NULL, '2025-02-03') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.proposition_offre (id, candidature_id, version, proposition_parent_id, salaire_base, temps_travail, emploi_du_temps, lieu_travail, date_prise_poste, date_limite_validite, date_emission, date_fin_contrat, type_contrat, periode_essai_mois, poste_id, departement_id, statut, motif_revision, validee_par, date_validation, date_acceptation) VALUES (5, 3, 1, NULL, 900000.00, 'Temps plein', NULL, 'Antananarivo', '2026-05-24', '2026-05-03', '2026-05-03', NULL, 'CDI', 6, 1, 1, 'ACCEPTEE', NULL, NULL, NULL, '2026-05-03') ON CONFLICT DO NOTHING;


--
-- Data for Name: employe; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.employe (id, matricule, candidat_id, proposition_id, nom, prenom, email, telephone, adresse, poste_id, departement_id, date_embauche, date_entree, date_fin_contrat, date_fin_periode_essai, statut, date_naissance, lieu_naissance, sexe, nationalite, numero_cin, numero_cnaps, situation_familiale, nombre_enfants, contact_urgence_nom, contact_urgence_telephone, dossier_minimal) VALUES (5, 'EMP-TEST-0002', 3, 5, 'Andria', 'Luc', 'luc.andria@gmail.com', NULL, NULL, 1, 1, '2026-05-03', '2026-05-24', NULL, '2026-11-23', 'EN_PERIODE_DESSAI', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.employe (id, matricule, candidat_id, proposition_id, nom, prenom, email, telephone, adresse, poste_id, departement_id, date_embauche, date_entree, date_fin_contrat, date_fin_periode_essai, statut, date_naissance, lieu_naissance, sexe, nationalite, numero_cin, numero_cnaps, situation_familiale, nombre_enfants, contact_urgence_nom, contact_urgence_telephone, dossier_minimal) VALUES (4, 'EMP-TEST-0001', 6, 4, 'Ranaivo', 'Aina', 'aina.ranaivo@gmail.com', '0341122334', 'Lot II A 12 Analakely', 1, 1, '2025-02-03', '2025-03-03', NULL, NULL, 'ACTIF', '1996-03-14', 'Antananarivo', 'F', 'Malagasy', NULL, NULL, 'MARIE', 2, '0347407375', NULL, false) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.employe (id, matricule, candidat_id, proposition_id, nom, prenom, email, telephone, adresse, poste_id, departement_id, date_embauche, date_entree, date_fin_contrat, date_fin_periode_essai, statut, date_naissance, lieu_naissance, sexe, nationalite, numero_cin, numero_cnaps, situation_familiale, nombre_enfants, contact_urgence_nom, contact_urgence_telephone, dossier_minimal) VALUES (6, 'AUTO-6', NULL, NULL, 'RH', 'Responsable', 'rh@rh.com', NULL, NULL, NULL, NULL, '2026-11-13', '2026-11-13', NULL, NULL, 'ACTIF', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.employe (id, matricule, candidat_id, proposition_id, nom, prenom, email, telephone, adresse, poste_id, departement_id, date_embauche, date_entree, date_fin_contrat, date_fin_periode_essai, statut, date_naissance, lieu_naissance, sexe, nationalite, numero_cin, numero_cnaps, situation_familiale, nombre_enfants, contact_urgence_nom, contact_urgence_telephone, dossier_minimal) VALUES (7, 'EMP-DEMO-SOLDE-01', NULL, NULL, 'Ranaivo', 'Aina', 'demo.solde.01@rh.com', NULL, NULL, NULL, NULL, '2025-03-13', '2025-03-13', NULL, NULL, 'ACTIF', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.employe (id, matricule, candidat_id, proposition_id, nom, prenom, email, telephone, adresse, poste_id, departement_id, date_embauche, date_entree, date_fin_contrat, date_fin_periode_essai, statut, date_naissance, lieu_naissance, sexe, nationalite, numero_cin, numero_cnaps, situation_familiale, nombre_enfants, contact_urgence_nom, contact_urgence_telephone, dossier_minimal) VALUES (8, 'EMP-DEMO-SOLDE-02', NULL, NULL, 'Andria', 'Luc', 'demo.solde.02@rh.com', NULL, NULL, NULL, NULL, '2026-06-03', '2026-06-03', NULL, NULL, 'ACTIF', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false) ON CONFLICT DO NOTHING;


--
-- Data for Name: absence; Type: TABLE DATA; Schema: mrtovo; Owner: -
--



--
-- Data for Name: affectation; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.affectation (id, employe_id, poste_id, departement_id, type, date_debut, date_fin, motif, statut) VALUES (4, 4, 1, 1, 'INITIALE', '2025-03-03', NULL, 'Embauche', 'ACTIVE') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.affectation (id, employe_id, poste_id, departement_id, type, date_debut, date_fin, motif, statut) VALUES (5, 5, 1, 1, 'INITIALE', '2026-05-24', NULL, 'Embauche', 'ACTIVE') ON CONFLICT DO NOTHING;


--
-- Data for Name: avantage_offre; Type: TABLE DATA; Schema: mrtovo; Owner: -
--



--
-- Data for Name: question_test; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.question_test (id, offre_emploi_id, ordre, enonce) VALUES (3, 1, 1, 'Mr Tovo') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.question_test (id, offre_emploi_id, ordre, enonce) VALUES (4, 1, 2, 'Balou') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.question_test (id, offre_emploi_id, ordre, enonce) VALUES (5, 1, 3, 'Nantenaina Elia Tolojanahary') ON CONFLICT DO NOTHING;


--
-- Data for Name: choix_question; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.choix_question (id, question_id, ordre, libelle, correct) VALUES (5, 3, 1, 'Prof Gestion', true) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.choix_question (id, question_id, ordre, libelle, correct) VALUES (6, 3, 2, 'Prof Milay be', false) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.choix_question (id, question_id, ordre, libelle, correct) VALUES (7, 4, 1, 'Mogli', true) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.choix_question (id, question_id, ordre, libelle, correct) VALUES (8, 4, 2, 'Copain an''Elia', false) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.choix_question (id, question_id, ordre, libelle, correct) VALUES (9, 5, 1, 'Je t''aime', true) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.choix_question (id, question_id, ordre, libelle, correct) VALUES (10, 5, 2, 'Je t''aime encore plus', false) ON CONFLICT DO NOTHING;


--
-- Data for Name: contrat; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.contrat (id, employe_id, reference, type_contrat, date_debut, date_fin, salaire, statut_contrat, document_url) VALUES (6, 4, 'CTR-TEST-0001', 'PERIODE_DESSAI', '2025-03-03', '2025-09-03', 1000000.00, 'TERMINE', NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.contrat (id, employe_id, reference, type_contrat, date_debut, date_fin, salaire, statut_contrat, document_url) VALUES (7, 4, 'CTR-TEST-0002', 'CDI', '2025-09-03', NULL, 1250000.00, 'ACTIF', NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.contrat (id, employe_id, reference, type_contrat, date_debut, date_fin, salaire, statut_contrat, document_url) VALUES (8, 5, 'CTR-TEST-0003', 'PERIODE_DESSAI', '2026-05-24', '2026-11-23', 900000.00, 'A_SIGNER', NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.contrat (id, employe_id, reference, type_contrat, date_debut, date_fin, salaire, statut_contrat, document_url) VALUES (9, 7, 'CTR-EMP-DEMO-SOLDE-01', 'CDI', '2025-03-13', NULL, 1250000.00, 'ACTIF', NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.contrat (id, employe_id, reference, type_contrat, date_debut, date_fin, salaire, statut_contrat, document_url) VALUES (10, 8, 'CTR-EMP-DEMO-SOLDE-02', 'CDI', '2026-06-03', NULL, 900000.00, 'ACTIF', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: decision_heures_sup; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.decision_heures_sup (id, employe_id, semaine_debut, statut, minutes_brutes, minutes_validees, commentaire, decide_par, date_decision) VALUES (2, 4, '2026-10-26', 'VALIDEE', 122, 122, NULL, 'rh@rh.com', '2026-11-04 19:09:24.525251') ON CONFLICT DO NOTHING;


--
-- Data for Name: type_conge; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.type_conge (id, code, libelle, decompte_solde, remunere, justificatif_requis, plafond_annuel_jours) VALUES (1, 'CONGE_PAYE', 'Congé payé', true, true, false, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.type_conge (id, code, libelle, decompte_solde, remunere, justificatif_requis, plafond_annuel_jours) VALUES (2, 'MALADIE', 'Congé maladie', false, true, true, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.type_conge (id, code, libelle, decompte_solde, remunere, justificatif_requis, plafond_annuel_jours) VALUES (3, 'PERMISSION', 'Permission exceptionnelle', false, true, true, 10) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.type_conge (id, code, libelle, decompte_solde, remunere, justificatif_requis, plafond_annuel_jours) VALUES (4, 'SANS_SOLDE', 'Congé sans solde', false, false, false, NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: demande_conge; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.demande_conge (id, employe_id, type_conge_id, date_debut, date_fin, nb_jours, motif, statut, date_demande, demande_par, avis_manager_par, date_avis_manager, commentaire_manager, decide_par, date_decision, commentaire_rh, justificatif_cle, justificatif_nom, justificatif_type) VALUES (1, 5, 1, '2026-11-16', '2026-11-20', 5, 'Vacances en famille', 'EN_ATTENTE_MANAGER', '2026-11-05 16:28:00.143147', 'script-test', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.demande_conge (id, employe_id, type_conge_id, date_debut, date_fin, nb_jours, motif, statut, date_demande, demande_par, avis_manager_par, date_avis_manager, commentaire_manager, decide_par, date_decision, commentaire_rh, justificatif_cle, justificatif_nom, justificatif_type) VALUES (2, 7, 1, '2026-08-31', '2026-09-04', 5, 'Conges d''ete (demo)', 'VALIDEE', '2026-11-13 10:00:03.550242', 'demo-solde', NULL, NULL, NULL, 'rh@rh.com', '2026-11-13 10:00:03.550286', 'Validee (demo)', NULL, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.demande_conge (id, employe_id, type_conge_id, date_debut, date_fin, nb_jours, motif, statut, date_demande, demande_par, avis_manager_par, date_avis_manager, commentaire_manager, decide_par, date_decision, commentaire_rh, justificatif_cle, justificatif_nom, justificatif_type) VALUES (3, 7, 1, '2026-10-05', '2026-10-06', 2, 'Rendez-vous personnel (demo)', 'VALIDEE', '2026-11-13 10:00:03.555142', 'demo-solde', NULL, NULL, NULL, 'rh@rh.com', '2026-11-13 10:00:03.555159', 'Validee (demo)', NULL, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.demande_conge (id, employe_id, type_conge_id, date_debut, date_fin, nb_jours, motif, statut, date_demande, demande_par, avis_manager_par, date_avis_manager, commentaire_manager, decide_par, date_decision, commentaire_rh, justificatif_cle, justificatif_nom, justificatif_type) VALUES (4, 7, 1, '2026-12-07', '2026-12-09', 3, 'Long week-end (demo)', 'EN_ATTENTE_RH', '2026-11-13 10:00:03.559125', 'demo-solde', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.demande_conge (id, employe_id, type_conge_id, date_debut, date_fin, nb_jours, motif, statut, date_demande, demande_par, avis_manager_par, date_avis_manager, commentaire_manager, decide_par, date_decision, commentaire_rh, justificatif_cle, justificatif_nom, justificatif_type) VALUES (5, 8, 1, '2026-10-19', '2026-10-20', 2, 'Demarche administrative (demo)', 'VALIDEE', '2026-11-13 10:00:03.577913', 'demo-solde', NULL, NULL, NULL, 'rh@rh.com', '2026-11-13 10:00:03.577932', 'Validee (demo)', NULL, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.demande_conge (id, employe_id, type_conge_id, date_debut, date_fin, nb_jours, motif, statut, date_demande, demande_par, avis_manager_par, date_avis_manager, commentaire_manager, decide_par, date_decision, commentaire_rh, justificatif_cle, justificatif_nom, justificatif_type) VALUES (6, 8, 1, '2026-11-23', '2026-11-27', 5, 'Conges de fin d''annee (demo)', 'EN_ATTENTE_MANAGER', '2026-11-13 10:00:03.582239', 'demo-solde', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.demande_conge (id, employe_id, type_conge_id, date_debut, date_fin, nb_jours, motif, statut, date_demande, demande_par, avis_manager_par, date_avis_manager, commentaire_manager, decide_par, date_decision, commentaire_rh, justificatif_cle, justificatif_nom, justificatif_type) VALUES (7, 8, 1, '2026-12-21', '2026-12-25', 5, 'Conges refusees (demo)', 'REFUSEE', '2026-11-13 10:00:03.587755', 'demo-solde', NULL, NULL, NULL, 'rh@rh.com', '2026-11-13 10:00:03.587777', 'Refusee (demo)', NULL, NULL, NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: document_salarie; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.document_salarie (id, employe_id, type_document, libelle, numero, date_emission, date_expiration, fichier_cle, nom_fichier_original, content_type, date_depot, depose_par) VALUES (6, 4, 'CIN', 'Carte d''identité nationale', '101 211 456 789', '2016-11-03', '2026-11-13', NULL, NULL, NULL, '2026-11-03 16:27:12.517627', 'script-test') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.document_salarie (id, employe_id, type_document, libelle, numero, date_emission, date_expiration, fichier_cle, nom_fichier_original, content_type, date_depot, depose_par) VALUES (7, 4, 'RIB', 'RIB BNI Madagascar', '00005 00001 12345678901 23', '2025-11-03', NULL, NULL, NULL, NULL, '2026-11-03 16:27:12.517627', 'script-test') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.document_salarie (id, employe_id, type_document, libelle, numero, date_emission, date_expiration, fichier_cle, nom_fichier_original, content_type, date_depot, depose_par) VALUES (8, 5, 'CERTIFICAT_MEDICAL', 'Visite médicale d''embauche', NULL, '2025-10-03', '2026-10-04', NULL, NULL, NULL, '2026-11-03 16:27:12.517627', 'script-test') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.document_salarie (id, employe_id, type_document, libelle, numero, date_emission, date_expiration, fichier_cle, nom_fichier_original, content_type, date_depot, depose_par) VALUES (9, 4, 'DIPLOME', 'le type', '01', '2026-11-03', '2026-11-08', NULL, NULL, NULL, '2026-11-03 19:39:26.531577', 'rh@rh.com') ON CONFLICT DO NOTHING;


--
-- Data for Name: droit_conge; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.droit_conge (id, employe_id, type_droit, jours, motif, mois_paie, montant, cree_par, date_creation) VALUES (1, 7, 'AJUSTEMENT', 3.00, 'Prime d''anciennete (demo)', NULL, NULL, 'demo-solde', '2026-11-13 10:00:03.545834') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.droit_conge (id, employe_id, type_droit, jours, motif, mois_paie, montant, cree_par, date_creation) VALUES (2, 7, 'INDEMNISATION', -2.00, 'Jours non pris (demo)', '2026-10', 83333.33, 'demo-solde', '2026-11-13 10:00:03.563177') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.droit_conge (id, employe_id, type_droit, jours, motif, mois_paie, montant, cree_par, date_creation) VALUES (3, 8, 'AJUSTEMENT', 2.00, 'Conge supplementaire negocie (demo)', NULL, NULL, 'demo-solde', '2026-11-13 10:00:03.574009') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.droit_conge (id, employe_id, type_droit, jours, motif, mois_paie, montant, cree_par, date_creation) VALUES (4, 8, 'INDEMNISATION', -1.00, 'Jour non pris (demo)', '2026-10', 30000.00, 'demo-solde', '2026-11-13 10:00:03.594328') ON CONFLICT DO NOTHING;


--
-- Data for Name: statut_evaluation; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.statut_evaluation (id, libelle) VALUES (1, 'En attente') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_evaluation (id, libelle) VALUES (2, 'Validé') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_evaluation (id, libelle) VALUES (3, 'Échoué') ON CONFLICT DO NOTHING;


--
-- Data for Name: evaluation; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.evaluation (id, candidature_id, note_test, statut_evaluation_id, date_soumission) VALUES (1, 3, NULL, 1, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.evaluation (id, candidature_id, note_test, statut_evaluation_id, date_soumission) VALUES (2, 4, 14.50, 1, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.evaluation (id, candidature_id, note_test, statut_evaluation_id, date_soumission) VALUES (3, 5, 16.00, 2, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.evaluation (id, candidature_id, note_test, statut_evaluation_id, date_soumission) VALUES (4, 6, 17.00, 2, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.evaluation (id, candidature_id, note_test, statut_evaluation_id, date_soumission) VALUES (5, 1, 14.00, 2, NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.evaluation (id, candidature_id, note_test, statut_evaluation_id, date_soumission) VALUES (6, 8, 0.00, 3, '2026-11-13 03:56:45.181789') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.evaluation (id, candidature_id, note_test, statut_evaluation_id, date_soumission) VALUES (7, 9, 20.00, 2, '2026-11-13 04:13:21.101785') ON CONFLICT DO NOTHING;


--
-- Data for Name: statut_entretien; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.statut_entretien (id, libelle) VALUES (1, 'Planifié') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_entretien (id, libelle) VALUES (2, 'Terminé') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.statut_entretien (id, libelle) VALUES (3, 'Annulé') ON CONFLICT DO NOTHING;


--
-- Data for Name: entretien; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.entretien (id, evaluation_id, date_entretien, lieu, commentaire, statut_entretien_id) VALUES (1, 3, '2026-11-05 10:00:00', 'Salle de réunion 1', NULL, 1) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.entretien (id, evaluation_id, date_entretien, lieu, commentaire, statut_entretien_id) VALUES (2, 4, '2026-11-02 14:00:00', 'Salle de réunion 1', 'Très bon profil, motivée.', 2) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.entretien (id, evaluation_id, date_entretien, lieu, commentaire, statut_entretien_id) VALUES (3, 5, '2026-11-14 08:18:00', 'Ao am_poko lesy ', NULL, 2) ON CONFLICT DO NOTHING;


--
-- Data for Name: grille_salariale; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.grille_salariale (id, poste_id, niveau, salaire_min, salaire_max, date_debut, date_fin) VALUES (1, 1, 'CONFIRME', 3000000.00, 4500000.00, '2025-01-01', NULL) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.grille_salariale (id, poste_id, niveau, salaire_min, salaire_max, date_debut, date_fin) VALUES (2, 1, 'SENIOR', 4500000.00, 6000000.00, '2025-01-01', NULL) ON CONFLICT DO NOTHING;


--
-- Data for Name: historique_candidature; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 1, 1, 'CANDIDATURE_DEPOSEE', NULL, 1, 'reprise des données', 'Candidature antérieure à l''historique : étapes suivantes non enregistrées', '2026-10-29 00:00:00' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 1);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 2, 2, 'CANDIDATURE_DEPOSEE', NULL, 1, 'reprise des données', 'Candidature antérieure à l''historique : étapes suivantes non enregistrées', '2026-10-30 00:00:00' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 2);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 3, 4, 'CANDIDATURE_DEPOSEE', NULL, 1, 'reprise des données', 'Candidature antérieure à l''historique : étapes suivantes non enregistrées', '2026-10-31 00:00:00' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 3);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 4, 5, 'CANDIDATURE_DEPOSEE', NULL, 1, 'reprise des données', 'Candidature antérieure à l''historique : étapes suivantes non enregistrées', '2026-11-01 00:00:00' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 4);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 5, 7, 'CANDIDATURE_DEPOSEE', NULL, 1, 'reprise des données', 'Candidature antérieure à l''historique : étapes suivantes non enregistrées', '2026-10-28 00:00:00' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 5);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 6, 3, 'CANDIDATURE_DEPOSEE', NULL, 1, 'reprise des données', 'Candidature antérieure à l''historique : étapes suivantes non enregistrées', '2026-10-31 00:00:00' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 6);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 7, 6, 'CANDIDATURE_DEPOSEE', NULL, 1, 'reprise des données', 'Candidature antérieure à l''historique : étapes suivantes non enregistrées', '2026-11-02 00:00:00' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 7);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 8, 1, 'PRESELECTION_RETENUE', 1, 2, 'rh@rh.com', NULL, '2026-11-13 03:10:39.513155' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 8);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 9, 1, 'NOTE_SAISIE', NULL, NULL, 'rh@rh.com', '14/20', '2026-11-13 03:12:05.475182' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 9);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 10, 1, 'TEST_VALIDE', 2, 3, 'rh@rh.com', '14/20', '2026-11-13 03:12:10.373122' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 10);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 11, 1, 'ENTRETIEN_PLANIFIE', NULL, NULL, 'rh@rh.com', '14/11/2026 à 08:18 · Ao am_poko lesy ', '2026-11-13 03:13:44.419622' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 11);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 12, 1, 'CANDIDAT_ACCEPTE', 3, 4, 'rh@rh.com', NULL, '2026-11-13 03:22:01.352696' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 12);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 13, 8, 'CANDIDATURE_DEPOSEE', NULL, 1, 'batmanhoodie@gmail.com', 'Offre « Développeur Fullstack Java / Vue.js »', '2026-11-13 03:52:41.133757' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 13);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 14, 8, 'PRESELECTION_RETENUE', 1, 2, 'rh@rh.com', NULL, '2026-11-13 03:55:58.736859' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 14);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 15, 8, 'TEST_PASSE', NULL, NULL, 'batmanhoodie@gmail.com', '0/20 · 0 bonne(s) réponse(s) sur 3', '2026-11-13 03:56:45.187252' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 15);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 16, 8, 'TEST_ECHOUE', 2, 5, 'rh@rh.com', '0/20', '2026-11-13 03:57:12.877618' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 16);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 17, 9, 'CANDIDATURE_DEPOSEE', NULL, 1, 'huhu@gmail.com', 'Offre « Développeur Fullstack Java / Vue.js »', '2026-11-13 04:11:33.401777' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 17);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 18, 9, 'PRESELECTION_RETENUE', 1, 2, 'rh@rh.com', NULL, '2026-11-13 04:11:54.87142' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 18);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 19, 9, 'TEST_PASSE', NULL, NULL, 'huhu@gmail.com', '20/20 · 3 bonne(s) réponse(s) sur 3', '2026-11-13 04:13:21.106442' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 19);
INSERT INTO mrtovo.historique_candidature (id, candidature_id, action, ancien_statut_id, nouveau_statut_id, auteur, details, date_action) SELECT 20, 9, 'TEST_VALIDE', 2, 3, 'rh@rh.com', '20/20', '2026-11-13 04:13:34.710333' WHERE NOT EXISTS (SELECT 1 FROM mrtovo.historique_candidature WHERE id = 20);


--
-- Data for Name: negociation; Type: TABLE DATA; Schema: mrtovo; Owner: -
--



--
-- Data for Name: historique_negociation; Type: TABLE DATA; Schema: mrtovo; Owner: -
--



--
-- Data for Name: historique_proposition; Type: TABLE DATA; Schema: mrtovo; Owner: -
--



--
-- Data for Name: regime_travail; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.regime_travail (id, code, libelle, tolerance_retard_minutes, date_creation, cree_par) VALUES (1, 'BUREAU_40H', 'Bureau 40 h (lundi au vendredi)', 10, '2026-11-04 18:58:01.38865', 'systeme') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.regime_travail (id, code, libelle, tolerance_retard_minutes, date_creation, cree_par) VALUES (2, 'SERVICE_SAMEDI_40H', 'Service 40 h (samedi matin inclus)', 10, '2026-11-04 18:58:01.38865', 'systeme') ON CONFLICT DO NOTHING;


--
-- Data for Name: horaire_regime; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (1, 1, 1, '08:00:00', '17:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (2, 1, 2, '08:00:00', '17:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (3, 1, 3, '08:00:00', '17:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (4, 1, 4, '08:00:00', '17:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (5, 1, 5, '08:00:00', '17:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (6, 2, 1, '08:00:00', '16:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (7, 2, 2, '08:00:00', '16:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (8, 2, 3, '08:00:00', '16:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (9, 2, 4, '08:00:00', '16:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (10, 2, 5, '08:00:00', '16:00:00', 60) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.horaire_regime (id, regime_id, jour_semaine, heure_debut, heure_fin, pause_minutes) VALUES (11, 2, 6, '08:00:00', '13:00:00', 0) ON CONFLICT DO NOTHING;


--
-- Data for Name: journal_audit; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (1, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:28:02.387056') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (2, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:29:34.937154') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (3, 5, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:30:09.652051') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (4, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:30:29.019927') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (5, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:30:51.188194') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (6, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:31:57.601492') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (7, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:34:10.790155') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (8, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:34:27.396935') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (9, 5, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:36:31.756816') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (10, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:37:41.885509') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (11, 4, 'MODIFICATION_IDENTITE', 'rh@rh.com', 'Contact d''urgence : — → 0347407375', '2026-11-03 19:37:55.750877') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (12, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:38:13.721771') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (13, 4, 'AJOUT_DOCUMENT', 'rh@rh.com', 'Diplôme — le type (n° 01)', '2026-11-03 19:39:26.538829') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (14, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:39:26.717319') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (15, 4, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-03 19:40:32.364218') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (16, 4, 'DECISION_HEURES_SUP', 'rh@rh.com', 'Semaine du 2026-10-26 : VALIDEE, 122 min sur 122', '2026-11-04 19:07:16.968344') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (17, 4, 'CORRECTION_POINTAGE', 'rh@rh.com', '2026-10-26 — motif : Test probleme
Sortie : 17:00 → 16:00', '2026-11-04 19:08:42.559632') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (18, 4, 'REOUVERTURE_HEURES_SUP', 'rh@rh.com', 'Semaine du 2026-10-26 : décision VALIDEE (122 min validées) annulée. Un pointage de la semaine a changé.', '2026-11-04 19:08:42.569361') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (19, 4, 'DECISION_HEURES_SUP', 'rh@rh.com', 'Semaine du 2026-10-26 : VALIDEE, 122 min sur 122', '2026-11-04 19:09:24.526855') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (20, 5, 'POINTAGE_ARRIVEE', 'luc.andria@gmail.com', '2026-11-13 : 09:51 → sortie non pointée', '2026-11-13 09:51:53.263575') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (21, 5, 'POINTAGE_DEPART', 'luc.andria@gmail.com', '2026-11-13 : 09:51 → 09:52', '2026-11-13 09:52:18.840913') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.journal_audit (id, employe_id, action, auteur, details, date_action) VALUES (22, 7, 'CONSULTATION_DOSSIER', 'rh@rh.com', NULL, '2026-11-13 10:00:50.918376') ON CONFLICT DO NOTHING;


--
-- Data for Name: planning_salarie; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.planning_salarie (id, employe_id, regime_id, date_debut, date_fin, affecte_par) VALUES (1, 5, 1, '2026-05-24', NULL, 'script-test') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.planning_salarie (id, employe_id, regime_id, date_debut, date_fin, affecte_par) VALUES (2, 4, 1, '2025-03-03', NULL, 'script-test') ON CONFLICT DO NOTHING;


--
-- Data for Name: pointage; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (2, 4, '2026-10-27', '08:00:00', '17:00:00', NULL, 'script-test', '2026-11-04 16:04:14.347843', NULL, NULL, 'RH') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (3, 4, '2026-10-28', '07:58:00', '17:02:00', NULL, 'script-test', '2026-11-04 16:04:14.347843', NULL, NULL, 'RH') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (4, 4, '2026-10-29', '08:00:00', '19:00:00', NULL, 'script-test', '2026-11-04 16:04:14.347843', NULL, NULL, 'RH') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (5, 4, '2026-10-30', '08:05:00', '17:00:00', NULL, 'script-test', '2026-11-04 16:04:14.347843', NULL, NULL, 'RH') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (6, 5, '2026-10-26', '09:40:00', '18:30:00', NULL, 'script-test', '2026-11-04 16:04:14.347843', NULL, NULL, 'RH') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (7, 5, '2026-10-27', '08:00:00', NULL, NULL, 'script-test', '2026-11-04 16:04:14.347843', NULL, NULL, 'RH') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (8, 5, '2026-10-28', '08:00:00', '15:30:00', 'Rendez-vous médical', 'script-test', '2026-11-04 16:04:14.347843', NULL, NULL, 'RH') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (9, 5, '2026-10-31', '09:00:00', '12:00:00', NULL, 'script-test', '2026-11-04 16:04:14.347843', NULL, NULL, 'RH') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (1, 4, '2026-10-26', '08:45:00', '16:00:00', NULL, 'script-test', '2026-11-04 16:04:14.347843', 'rh@rh.com', '2026-11-04 19:08:42.556913', 'RH') ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.pointage (id, employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par, date_saisie, modifie_par, date_modification, source) VALUES (10, 5, '2026-11-13', '09:51:00', '09:52:00', NULL, 'luc.andria@gmail.com', '2026-11-13 09:51:53.255044', NULL, NULL, 'SALARIE') ON CONFLICT DO NOTHING;


--
-- Data for Name: reponse_candidat; Type: TABLE DATA; Schema: mrtovo; Owner: -
--

INSERT INTO mrtovo.reponse_candidat (id, evaluation_id, question_id, choix_id) VALUES (1, 6, 3, 6) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.reponse_candidat (id, evaluation_id, question_id, choix_id) VALUES (2, 6, 4, 8) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.reponse_candidat (id, evaluation_id, question_id, choix_id) VALUES (3, 6, 5, 10) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.reponse_candidat (id, evaluation_id, question_id, choix_id) VALUES (4, 7, 3, 5) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.reponse_candidat (id, evaluation_id, question_id, choix_id) VALUES (5, 7, 4, 7) ON CONFLICT DO NOTHING;
INSERT INTO mrtovo.reponse_candidat (id, evaluation_id, question_id, choix_id) VALUES (6, 7, 5, 9) ON CONFLICT DO NOTHING;


--
-- Data for Name: departement; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.departement (id_departement, nom) VALUES (1, 'Informatique') ON CONFLICT DO NOTHING;
INSERT INTO public.departement (id_departement, nom) VALUES (2, 'Ressources Humaines') ON CONFLICT DO NOTHING;
INSERT INTO public.departement (id_departement, nom) VALUES (3, 'Finance') ON CONFLICT DO NOTHING;
INSERT INTO public.departement (id_departement, nom) VALUES (4, 'Direction') ON CONFLICT DO NOTHING;
INSERT INTO public.departement (id_departement, nom) VALUES (5, 'Commercial') ON CONFLICT DO NOTHING;


--
-- Data for Name: niveau_poste; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.niveau_poste (id_niveau, libelle, rang) VALUES (1, 'Junior', 1) ON CONFLICT DO NOTHING;
INSERT INTO public.niveau_poste (id_niveau, libelle, rang) VALUES (2, 'Confirmé', 2) ON CONFLICT DO NOTHING;
INSERT INTO public.niveau_poste (id_niveau, libelle, rang) VALUES (3, 'Senior', 3) ON CONFLICT DO NOTHING;


--
-- Data for Name: poste; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.poste (id_poste, description, intitule) VALUES (1, 'Développement du portefeuille clients', 'Commercial') ON CONFLICT DO NOTHING;
INSERT INTO public.poste (id_poste, description, intitule) VALUES (2, 'Conception, développement et maintenance des applications', 'Développeur') ON CONFLICT DO NOTHING;
INSERT INTO public.poste (id_poste, description, intitule) VALUES (3, 'Gestion administrative et accompagnement du personnel', 'Chargé RH') ON CONFLICT DO NOTHING;
INSERT INTO public.poste (id_poste, description, intitule) VALUES (4, 'Tenue de la comptabilité et suivi financier', 'Comptable') ON CONFLICT DO NOTHING;


--
-- Data for Name: employe; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.employe (id_employe, actif, date_embauche, matricule, nom, id_departement, id_niveau, id_poste) VALUES (1, true, '2022-03-10', 'EMP-001', 'Rakoto Jean', 1, 1, 2) ON CONFLICT DO NOTHING;
INSERT INTO public.employe (id_employe, actif, date_embauche, matricule, nom, id_departement, id_niveau, id_poste) VALUES (2, true, '2022-03-10', 'EMP-002', 'Rasoa Marie', 1, 3, 2) ON CONFLICT DO NOTHING;
INSERT INTO public.employe (id_employe, actif, date_embauche, matricule, nom, id_departement, id_niveau, id_poste) VALUES (3, true, '2022-03-10', 'EMP-003', 'Andry Faly', 1, 2, 2) ON CONFLICT DO NOTHING;


--
-- Data for Name: utilisateur; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (1, 'manager.informatique@rh.com', '$2a$10$RRX5cUQyeKwpPUu2hY7zpe6WgWOmoka86Ws8/8Kda.zyJdt.XM3gC', 'Manager Informatique', 'MANAGER', NULL, 1, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (2, 'manager.rh@rh.com', '$2a$10$DgzFY0omoCG/h7IgTsN/keHlG5F0GNJ4TeGX8h6JTJjCDbG7/ZLfS', 'Manager Ressources Humaines', 'MANAGER', NULL, 2, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (3, 'manager.finance@rh.com', '$2a$10$ncXLxxKCPAmjJ16BLg7up.lBgDgwACIhA7oVNDWIGZva4z7rNnG/.', 'Manager Finance', 'MANAGER', NULL, 3, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (4, 'manager.direction@rh.com', '$2a$10$G187l8oRj6RDfUWEmdia2ud9rrVMp.EhNm/xM563rjqm24d1qoclC', 'Manager Direction', 'MANAGER', NULL, 4, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (5, 'manager.commercial@rh.com', '$2a$10$idsy9Q8mYFu6tPoCOCc/9OfSLTAbPJFGXu39p9zUTBeGrfkAH57u6', 'Manager Commercial', 'MANAGER', NULL, 5, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (7, 'finance@rh.com', '$2a$10$Jms3k.Gid39Y9lLAa6isSe9ThP79RyEXo7Iq6k02W6QlWMRkzaupC', 'Responsable Finance', 'FINANCE', NULL, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (8, 'direction@rh.com', '$2a$10$RWLz7aqZWYt2Rn5/4a8FgumE2UgPiT0TZizkhA/h6.DSq7mzF3R4C', 'Directeur Général', 'DIRECTION', NULL, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (9, 'luc.andria@gmail.com', '$2a$10$VmWAInOVL6riKFnSUJMlG.jtgKCMj0Aah2iRPDIJEhuhismQuUbLa', 'Luc Andria', 'EMPLOYE', NULL, NULL, 5) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (10, 'aina.ranaivo@gmail.com', '$2a$10$VmWAInOVL6riKFnSUJMlG.jtgKCMj0Aah2iRPDIJEhuhismQuUbLa', 'Aina Ranaivo', 'EMPLOYE', NULL, NULL, 4) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (11, 'batmanhoodie@gmail.com', '$2a$10$XNm0vTJ5S0tFp2F9M9q3x.VGvsXRekIIhw3.q3FpOymqp5WGhJ5m.', 'Alexandre RAK', 'CANDIDAT', 8, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (12, 'huhu@gmail.com', '$2a$10$pSWf1OPh80/P6Jsz7wnEyueBSazTWCXIpq1f.yGtmL9jn4RkOfKze', 'einf epifj', 'CANDIDAT', 9, NULL, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (13, 'emp@mail.com', '$2a$10$iC55U81MB2wsml6C9W7nX.5aI/2t3HWq6IG2ihubIgk.NeLW3Fpma', 'Employee Finance', 'EMPLOYE', NULL, 3, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (6, 'rh@rh.com', '$2a$10$82VVY/hlGWD8uXBTwYiQVed0J5EOPJ.ziP5mfmvC9CoAzmJF18JPW', 'Responsable RH', 'RH', NULL, NULL, 6) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (14, 'demo.solde.01@rh.com', '$2a$10$swOqKihiLlVhT7INlXvSMevQwctu6AJ3x.MZzxglvajwnJbdg5GiC', 'Aina Ranaivo', 'EMPLOYE', NULL, NULL, 7) ON CONFLICT DO NOTHING;
INSERT INTO public.utilisateur (id_utilisateur, email, mot_de_passe, nom, role, id_candidat, id_departement, id_employe) VALUES (15, 'demo.solde.02@rh.com', '$2a$10$Z5wSNWyE5QscVXfeRM9ueeqBBcXPkutbbpEJDNh9NXfYpQdCGT7ki', 'Luc Andria', 'EMPLOYE', NULL, NULL, 8) ON CONFLICT DO NOTHING;


--
-- Data for Name: demande_recrutement; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: grille_salariale; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (1, '2026-01-01', NULL, 1100000.00, 700000.00, 1, 1) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (2, '2026-01-01', NULL, 1650000.00, 1050000.00, 2, 1) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (3, '2026-01-01', NULL, 2200000.00, 1400000.00, 3, 1) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (4, '2026-01-01', NULL, 1800000.00, 1200000.00, 1, 2) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (5, '2026-01-01', NULL, 2700000.00, 1800000.00, 2, 2) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (6, '2026-01-01', NULL, 3600000.00, 2400000.00, 3, 2) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (7, '2026-01-01', NULL, 1200000.00, 800000.00, 1, 3) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (8, '2026-01-01', NULL, 1800000.00, 1200000.00, 2, 3) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (9, '2026-01-01', NULL, 2400000.00, 1600000.00, 3, 3) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (10, '2026-01-01', NULL, 1400000.00, 900000.00, 1, 4) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (11, '2026-01-01', NULL, 2100000.00, 1350000.00, 2, 4) ON CONFLICT DO NOTHING;
INSERT INTO public.grille_salariale (id_grille, date_debut, date_fin, salaire_max, salaire_min, id_niveau, id_poste) VALUES (12, '2026-01-01', NULL, 2800000.00, 1800000.00, 3, 4) ON CONFLICT DO NOTHING;


--
-- Data for Name: ligne_demande; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: validation; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Name: absence_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.absence_id_seq', 1, false);


--
-- Name: affectation_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.affectation_id_seq', 5, true);


--
-- Name: avantage_offre_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.avantage_offre_id_seq', 1, false);


--
-- Name: candidat_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.candidat_id_seq', 9, true);


--
-- Name: candidature_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.candidature_id_seq', 9, true);


--
-- Name: choix_question_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.choix_question_id_seq', 10, true);


--
-- Name: contrat_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.contrat_id_seq', 10, true);


--
-- Name: decision_heures_sup_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.decision_heures_sup_id_seq', 2, true);


--
-- Name: demande_conge_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.demande_conge_id_seq', 7, true);


--
-- Name: departement_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.departement_id_seq', 30, true);


--
-- Name: document_salarie_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.document_salarie_id_seq', 9, true);


--
-- Name: droit_conge_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.droit_conge_id_seq', 4, true);


--
-- Name: employe_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.employe_id_seq', 8, true);


--
-- Name: entretien_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.entretien_id_seq', 3, true);


--
-- Name: evaluation_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.evaluation_id_seq', 7, true);


--
-- Name: grille_salariale_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.grille_salariale_id_seq', 60, true);


--
-- Name: historique_candidature_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.historique_candidature_id_seq', 20, true);


--
-- Name: historique_negociation_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.historique_negociation_id_seq', 1, false);


--
-- Name: historique_proposition_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.historique_proposition_id_seq', 1, false);


--
-- Name: horaire_regime_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.horaire_regime_id_seq', 143, true);


--
-- Name: journal_audit_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.journal_audit_id_seq', 22, true);


--
-- Name: negociation_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.negociation_id_seq', 1, false);


--
-- Name: offre_emploi_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.offre_emploi_id_seq', 1, true);


--
-- Name: planning_salarie_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.planning_salarie_id_seq', 2, true);


--
-- Name: pointage_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.pointage_id_seq', 10, true);


--
-- Name: poste_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.poste_id_seq', 1, true);


--
-- Name: proposition_offre_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.proposition_offre_id_seq', 5, true);


--
-- Name: question_test_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.question_test_id_seq', 5, true);


--
-- Name: regime_travail_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.regime_travail_id_seq', 2, true);


--
-- Name: reponse_candidat_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.reponse_candidat_id_seq', 6, true);


--
-- Name: seq_contrat_reference; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.seq_contrat_reference', 1, false);


--
-- Name: seq_matricule; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.seq_matricule', 1, false);


--
-- Name: statut_candidature_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.statut_candidature_id_seq', 5, true);


--
-- Name: statut_entretien_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.statut_entretien_id_seq', 3, true);


--
-- Name: statut_evaluation_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.statut_evaluation_id_seq', 3, true);


--
-- Name: statut_offre_emploi_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.statut_offre_emploi_id_seq', 3, true);


--
-- Name: type_conge_id_seq; Type: SEQUENCE SET; Schema: mrtovo; Owner: -
--

SELECT pg_catalog.setval('mrtovo.type_conge_id_seq', 4, true);


--
-- Name: demande_recrutement_id_demande_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.demande_recrutement_id_demande_seq', 1, false);


--
-- Name: departement_id_departement_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.departement_id_departement_seq', 5, true);


--
-- Name: employe_id_employe_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.employe_id_employe_seq', 3, true);


--
-- Name: grille_salariale_id_grille_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.grille_salariale_id_grille_seq', 12, true);


--
-- Name: ligne_demande_id_ligne_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.ligne_demande_id_ligne_seq', 1, false);


--
-- Name: niveau_poste_id_niveau_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.niveau_poste_id_niveau_seq', 3, true);


--
-- Name: poste_id_poste_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.poste_id_poste_seq', 4, true);


--
-- Name: utilisateur_id_utilisateur_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.utilisateur_id_utilisateur_seq', 15, true);


--
-- Name: validation_id_validation_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.validation_id_validation_seq', 1, false);


--
-- PostgreSQL database dump complete
--

-- Verification des cles etrangeres retablie.
SET session_replication_role = DEFAULT;
