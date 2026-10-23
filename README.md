# Expression du besoin en personnel

Application Spring Boot (API) + Vue 3 (interface) pour exprimer, chiffrer, valider et ouvrir des recrutements.

## Lancer le projet

1. **Base de données** : PostgreSQL, base `rh_db` (connexion dans `backend/src/main/resources/application.properties`).
   - **Bloc 1** (demandes, schéma `public`) : Hibernate crée les tables (`ddl-auto=update`), `DataInitializer` insère les données de démo. Script de référence : `database/schema.sql`.
   - **Bloc 2** (candidatures, schéma `mrtovo`) : `schema.sql` + `data.sql` sont exécutés à chaque démarrage mais ne créent que ce qui manque (données conservées). Pour repartir de zéro : `DROP SCHEMA mrtovo CASCADE;`.
2. **Backend** : `cd backend && ./mvnw spring-boot:run` (port 8012). Les données de démo sont insérées au premier démarrage.
3. **Frontend** : `cd frontend && npm install && npm run dev` puis ouvrir http://localhost:5173

## Comptes de démonstration

| Rôle | Email | Mot de passe |
|---|---|---|
| Manager (un par département) | manager.informatique@rh.com, manager.rh@rh.com, manager.finance@rh.com, manager.direction@rh.com, manager.commercial@rh.com | manager123 |
| RH | rh@rh.com | rh123 |
| Finance | finance@rh.com | finance123 |
| Direction | direction@rh.com | direction123 |

## Règles métier

- **Poste** : catalogue indépendant des départements ; chaque demande porte son département.
- **Type de besoin** (`CREATION` / `REMPLACEMENT`) ≠ **motif** (texte libre : pourquoi on a besoin des personnes).
- **Effectifs par niveau** : une demande contient des lignes (ex. 2 juniors + 1 senior).
- **Grille salariale** : par poste **et** niveau, avec période de validité.
  - Création : le RH définit une nouvelle fourchette (l'ancienne est clôturée, l'historique est gardé) ou reprend celle en vigueur.
  - Remplacement : la grille existante est utilisée, aucune nouvelle fourchette.
- **Budget** : le manager saisit un budget total demandé ; le RH saisit le coût estimé par personne (dans la fourchette de la grille) ; le coût total est calculé et comparé au budget.
- **Workflow** : RH → Finance → Direction. RH et Finance donnent un **avis** (même défavorable, la demande continue) ; la Direction a le **dernier mot** sur la demande entière. Seul le RH ouvre le recrutement, uniquement si la demande est autorisée.
- **Historique** : chaque avis est une ligne distincte dans `validation`, jamais écrasée.

## Bloc 2 : suivi des candidatures

Menu **Candidatures** (rôle RH) : liste des offres, puis tableau kanban par offre.
Chaque demande crée son offre d'emploi : **En attente** à la création, **Fermée** si la Direction refuse, **Ouverte** à l'ouverture du recrutement.
Parcours d'une candidature : À traiter → (présélection) Test → (note /20 + décision) Attente entretien → Accepté / Refusé.
Les identifiants de statuts sont fixés dans `data.sql` et repris dans `Statuts.java` et `frontend/src/services/api.js`.
