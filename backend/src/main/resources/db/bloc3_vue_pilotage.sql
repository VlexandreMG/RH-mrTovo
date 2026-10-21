-- =====================================================================
-- MRTOVO - Bloc 3 : VUE DE PILOTAGE (optionnelle)
--
-- Cette vue n'est PAS creee automatiquement par l'application : une vue
-- PostgreSQL empeche la modification du type d'une colonne dont elle
-- depend, ce qui perturberait les applications des autres blocs.
-- Creation manuelle :
--   psql -h 127.0.0.1 -U mrtovo -d rh_db -f bloc3_vue_pilotage.sql
-- =====================================================================

CREATE OR REPLACE VIEW mrtovo.v_pipeline_embauche AS
SELECT p.id                     AS proposition_id,
       p.version                AS version,
       p.statut                 AS proposition_statut,
       c.id                     AS candidature_id,
       c.statut                 AS candidature_statut,
       cand.nom                 AS candidat_nom,
       cand.prenom              AS candidat_prenom,
       cand.email               AS candidat_email,
       o.titre                  AS offre_titre,
       po.titre                 AS poste_titre,
       d.nom                    AS departement_nom,
       p.salaire_base           AS salaire_base,
       p.type_contrat           AS type_contrat,
       p.date_emission          AS date_emission,
       p.date_limite_validite   AS date_limite_validite,
       p.date_acceptation       AS date_acceptation,
       e.id                     AS employe_id,
       e.matricule              AS matricule
  FROM mrtovo.proposition_offre p
  JOIN mrtovo.candidature c ON c.id = p.candidature_id
  JOIN mrtovo.candidat cand ON cand.id = c.candidat_id
  JOIN mrtovo.offre_emploi o ON o.id = c.offre_emploi_id
  LEFT JOIN mrtovo.poste po ON po.id = p.poste_id
  LEFT JOIN mrtovo.departement d ON d.id = p.departement_id
  LEFT JOIN mrtovo.employe e ON e.proposition_id = p.id;
