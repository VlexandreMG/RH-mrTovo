-- Bloc 7 : jeu de donnees de TEST pour les conges et absences.
-- A lancer APRES bloc5-donnees-test.sql et bloc6-donnees-test.sql, et apres un premier demarrage du backend.
-- Cree les comptes des deux salaries de test (mot de passe : salarie123) et une demande en attente du manager.
-- Execution : docker exec -i postgres-16 psql -U mrtovo -d rh_db < database/bloc7-donnees-test.sql
-- Rejouable : ne fait rien si ces comptes existent deja.

BEGIN;

DO $$
DECLARE
    -- Empreinte BCrypt de « salarie123 ».
    v_empreinte TEXT := '$2a$10$VmWAInOVL6riKFnSUJMlG.jtgKCMj0Aah2iRPDIJEhuhismQuUbLa';
    v_luc BIGINT := (SELECT id FROM mrtovo.employe WHERE matricule = 'EMP-TEST-0002');
BEGIN
    IF v_luc IS NULL THEN
        RAISE EXCEPTION 'Lancer d''abord database/bloc5-donnees-test.sql';
    END IF;
    IF EXISTS (SELECT 1 FROM public.utilisateur WHERE id_employe = v_luc) THEN
        RAISE NOTICE 'Donnees de test deja presentes : rien a faire.';
        RETURN;
    END IF;

    INSERT INTO public.utilisateur (nom, email, mot_de_passe, role, id_employe)
    SELECT prenom || ' ' || nom, lower(email), v_empreinte, 'EMPLOYE', id
    FROM mrtovo.employe
    WHERE matricule IN ('EMP-TEST-0001', 'EMP-TEST-0002');

    -- Conge paye de Luc dans deux semaines, du lundi au vendredi : 5 jours.
    INSERT INTO mrtovo.demande_conge (employe_id, type_conge_id, date_debut, date_fin, nb_jours, motif, statut, demande_par)
    VALUES (v_luc, 1, date_trunc('week', CURRENT_DATE)::date + 14, date_trunc('week', CURRENT_DATE)::date + 18, 5,
            'Vacances en famille', 'EN_ATTENTE_MANAGER', 'script-test');

    RAISE NOTICE 'Comptes salaries et demande de test crees.';
END $$;

COMMIT;
