-- Bloc 5 : jeu de donnees de TEST pour le dossier administratif du salarie.
-- Cree 2 salaries (a partir des candidatures 6 et 3) avec contrats, affectations et documents.
-- Les dates sont relatives a CURRENT_DATE pour que les alertes apparaissent toujours.
-- Execution : docker exec -i postgres-16 psql -U mrtovo -d rh_db < database/bloc5-donnees-test.sql
-- Rejouable : ne fait rien si les salaries de test existent deja.

BEGIN;
SET search_path TO mrtovo;

DO $$
DECLARE
    v_poste BIGINT := (SELECT MIN(id) FROM poste);
    v_dep   BIGINT := (SELECT MIN(id) FROM departement);
    v_prop1 BIGINT;
    v_prop2 BIGINT;
    v_emp1  BIGINT;
    v_emp2  BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM employe WHERE matricule IN ('EMP-TEST-0001', 'EMP-TEST-0002')) THEN
        RAISE NOTICE 'Donnees de test deja presentes : rien a faire.';
        RETURN;
    END IF;

    -- ---------- Salarie 1 : Aina Ranaivo, en poste depuis ~20 mois, CE puis CDI avec augmentation ----------
    INSERT INTO proposition_offre (candidature_id, salaire_base, temps_travail, lieu_travail,
                                   date_prise_poste, date_limite_validite, date_emission,
                                   type_contrat, periode_essai_mois, statut, poste_id, departement_id, date_acceptation)
    VALUES (6, 1000000, 'Temps plein', 'Antananarivo',
            CURRENT_DATE - INTERVAL '20 months', CURRENT_DATE - INTERVAL '21 months', CURRENT_DATE - INTERVAL '21 months',
            'CDI', 6, 'ACCEPTEE', v_poste, v_dep, CURRENT_DATE - INTERVAL '21 months')
    RETURNING id INTO v_prop1;

    INSERT INTO employe (matricule, candidat_id, proposition_id, nom, prenom, email, telephone, adresse,
                         poste_id, departement_id, date_embauche, date_entree, statut,
                         date_naissance, lieu_naissance, sexe, nationalite, situation_familiale, nombre_enfants)
    SELECT 'EMP-TEST-0001', c.id, v_prop1, c.nom, c.prenom, c.email, '0341122334', 'Lot II A 12 Analakely',
           v_poste, v_dep, CURRENT_DATE - INTERVAL '21 months', CURRENT_DATE - INTERVAL '20 months', 'ACTIF',
           DATE '1996-03-14', 'Antananarivo', 'F', 'Malagasy', 'MARIE', 2
    FROM candidature ca JOIN candidat c ON c.id = ca.candidat_id WHERE ca.id = 6
    RETURNING id INTO v_emp1;

    INSERT INTO contrat (employe_id, reference, type_contrat, date_debut, date_fin, salaire, statut_contrat) VALUES
        (v_emp1, 'CTR-TEST-0001', 'PERIODE_DESSAI', CURRENT_DATE - INTERVAL '20 months', CURRENT_DATE - INTERVAL '14 months', 1000000, 'TERMINE'),
        (v_emp1, 'CTR-TEST-0002', 'CDI',            CURRENT_DATE - INTERVAL '14 months', NULL,                                1250000, 'ACTIF');

    INSERT INTO affectation (employe_id, poste_id, departement_id, type, date_debut, motif, statut)
    VALUES (v_emp1, v_poste, v_dep, 'INITIALE', CURRENT_DATE - INTERVAL '20 months', 'Embauche', 'ACTIVE');

    -- Documents sans fichier joint (fiches) : une CIN qui expire dans 10 jours, un RIB permanent.
    INSERT INTO document_salarie (employe_id, type_document, libelle, numero, date_emission, date_expiration, depose_par) VALUES
        (v_emp1, 'CIN', 'Carte d''identité nationale', '101 211 456 789', CURRENT_DATE - INTERVAL '10 years', CURRENT_DATE + 10, 'script-test'),
        (v_emp1, 'RIB', 'RIB BNI Madagascar', '00005 00001 12345678901 23', CURRENT_DATE - INTERVAL '1 year', NULL, 'script-test');

    -- ---------- Salarie 2 : Luc Andria, arrive il y a ~5 mois, periode d'essai qui finit dans 20 jours ----------
    INSERT INTO proposition_offre (candidature_id, salaire_base, temps_travail, lieu_travail,
                                   date_prise_poste, date_limite_validite, date_emission,
                                   type_contrat, periode_essai_mois, statut, poste_id, departement_id, date_acceptation)
    VALUES (3, 900000, 'Temps plein', 'Antananarivo',
            CURRENT_DATE - INTERVAL '5 months' - INTERVAL '10 days', CURRENT_DATE - INTERVAL '6 months', CURRENT_DATE - INTERVAL '6 months',
            'CDI', 6, 'ACCEPTEE', v_poste, v_dep, CURRENT_DATE - INTERVAL '6 months')
    RETURNING id INTO v_prop2;

    INSERT INTO employe (matricule, candidat_id, proposition_id, nom, prenom, email,
                         poste_id, departement_id, date_embauche, date_entree, date_fin_periode_essai, statut)
    SELECT 'EMP-TEST-0002', c.id, v_prop2, c.nom, c.prenom, c.email,
           v_poste, v_dep, CURRENT_DATE - INTERVAL '6 months', CURRENT_DATE - INTERVAL '5 months' - INTERVAL '10 days',
           CURRENT_DATE + 20, 'EN_PERIODE_DESSAI'
    FROM candidature ca JOIN candidat c ON c.id = ca.candidat_id WHERE ca.id = 3
    RETURNING id INTO v_emp2;

    INSERT INTO contrat (employe_id, reference, type_contrat, date_debut, date_fin, salaire, statut_contrat)
    VALUES (v_emp2, 'CTR-TEST-0003', 'PERIODE_DESSAI', CURRENT_DATE - INTERVAL '5 months' - INTERVAL '10 days', CURRENT_DATE + 20, 900000, 'A_SIGNER');

    INSERT INTO affectation (employe_id, poste_id, departement_id, type, date_debut, motif, statut)
    VALUES (v_emp2, v_poste, v_dep, 'INITIALE', CURRENT_DATE - INTERVAL '5 months' - INTERVAL '10 days', 'Embauche', 'ACTIVE');

    -- Un certificat medical deja expire.
    INSERT INTO document_salarie (employe_id, type_document, libelle, date_emission, date_expiration, depose_par)
    VALUES (v_emp2, 'CERTIFICAT_MEDICAL', 'Visite médicale d''embauche', CURRENT_DATE - INTERVAL '13 months', CURRENT_DATE - 30, 'script-test');

    UPDATE candidature SET statut = 'EMBAUCHE' WHERE id IN (3, 6);

    RAISE NOTICE 'Salaries de test crees : % et %', v_emp1, v_emp2;
END $$;

COMMIT;
