-- Bloc 6 : jeu de donnees de TEST pour le temps de travail.
-- A lancer APRES bloc5-donnees-test.sql : il utilise les salaries EMP-TEST-0001 et EMP-TEST-0002.
-- Les dates sont relatives a la semaine courante pour que les ecrans aient toujours quelque chose a montrer.
-- Execution : docker exec -i postgres-16 psql -U mrtovo -d rh_db < database/bloc6-donnees-test.sql
-- Rejouable : ne fait rien si le planning de test existe deja.

BEGIN;
SET search_path TO mrtovo;

DO $$
DECLARE
    v_aina BIGINT := (SELECT id FROM employe WHERE matricule = 'EMP-TEST-0001');
    v_luc  BIGINT := (SELECT id FROM employe WHERE matricule = 'EMP-TEST-0002');
    -- Lundi de la semaine derniere : semaine terminee, donc ses heures sup sont decidables.
    v_lundi DATE := date_trunc('week', CURRENT_DATE)::date - 7;
BEGIN
    IF v_aina IS NULL OR v_luc IS NULL THEN
        RAISE EXCEPTION 'Lancer d''abord database/bloc5-donnees-test.sql';
    END IF;
    IF EXISTS (SELECT 1 FROM planning_salarie WHERE employe_id IN (v_aina, v_luc)) THEN
        RAISE NOTICE 'Donnees de test deja presentes : rien a faire.';
        RETURN;
    END IF;

    INSERT INTO planning_salarie (employe_id, regime_id, date_debut, affecte_par)
    SELECT id, 1, date_entree, 'script-test' FROM employe WHERE id IN (v_aina, v_luc);

    -- Aina : 45 min de retard lundi, 2 h d'heures sup jeudi => cas de compensation complete.
    INSERT INTO pointage (employe_id, date_pointage, heure_entree, heure_sortie, saisi_par) VALUES
        (v_aina, v_lundi,     '08:45', '17:00', 'script-test'),
        (v_aina, v_lundi + 1, '08:00', '17:00', 'script-test'),
        (v_aina, v_lundi + 2, '07:58', '17:02', 'script-test'),
        (v_aina, v_lundi + 3, '08:00', '19:00', 'script-test'),
        (v_aina, v_lundi + 4, '08:05', '17:00', 'script-test');

    -- Luc : plusieurs anomalies (gros retard, sortie oubliee, depart anticipe, absence, samedi travaille).
    INSERT INTO pointage (employe_id, date_pointage, heure_entree, heure_sortie, commentaire, saisi_par) VALUES
        (v_luc, v_lundi,     '09:40', '18:30', NULL, 'script-test'),
        (v_luc, v_lundi + 1, '08:00', NULL,    NULL, 'script-test'),
        (v_luc, v_lundi + 2, '08:00', '15:30', 'Rendez-vous médical', 'script-test'),
        (v_luc, v_lundi + 5, '09:00', '12:00', NULL, 'script-test');

    RAISE NOTICE 'Planning et pointages de test crees pour la semaine du %', v_lundi;
END $$;

COMMIT;
