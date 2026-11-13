# Expression du besoin en personnel

Application Spring Boot (API) + Vue 3 (interface) pour exprimer, chiffrer, valider et ouvrir des recrutements.

## Lancer le projet

1. **Base de données** : démarrer le service PostgreSQL et créer une seule fois la base `rh_db` ainsi que l'utilisateur configuré dans `backend/src/main/resources/application.properties`. Spring Boot ne crée pas le serveur PostgreSQL ni la base elle-même.
   - **Bloc 1** (schéma `public`) : Hibernate crée/met à jour les tables (`ddl-auto=update`) et `DataInitializer` ajoute les données de démonstration si les tables concernées sont vides.
   - **Blocs 2 et 3** (schéma `mrtovo`) : `schema.sql` et `data.sql` sont exécutés au démarrage. Ils créent les tables manquantes et ajoutent les références de démonstration sans supprimer les données existantes.
2. **Backend** : double-cliquer sur [`demarrer.bat`](./demarrer.bat), ou exécuter `backend/mvnw.cmd spring-boot:run` dans un terminal. L'API écoute sur le port 8080. La première compilation peut nécessiter un accès Internet pour Maven.
3. **Frontend** : depuis le dossier `frontend`, exécuter `npm install` une première fois puis `npm run dev`, et ouvrir http://localhost:5173.

Pour réinitialiser les données de l'application, arrêter le backend puis supprimer les schémas `public` et `mrtovo` de `rh_db` avec un client PostgreSQL. Au prochain lancement, Spring Boot recrée les tables et réinsère les données de démarrage. Cette opération efface toutes les données applicatives.

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
Parcours d'une candidature : À traiter → (présélection) Test → (test en ligne + décision) Attente entretien → Accepté / Refusé.
**Test en ligne** : le RH écrit le QCM de chaque offre (bouton **Questionnaire du test** du kanban ; une seule bonne réponse par question). Un candidat ne peut être retenu que si ce questionnaire existe. Le candidat crée son compte (page **Créer un compte**, avec l'email de sa candidature : le compte se relie seul à son dossier), puis passe le test depuis **Mon espace**, en une seule tentative. La note sur 20 est calculée par le serveur (bonnes réponses / questions × 20) et ne se modifie pas ; le RH voit la note et le détail des réponses, puis valide ou refuse. Le questionnaire se verrouille dès qu'un candidat a répondu.
Documentation : [`docs/bloc2-test/BLOC2-TEST-EN-LIGNE.md`](./docs/bloc2-test/BLOC2-TEST-EN-LIGNE.md). Plan : [`docs/bloc2-test/BLOC2-TEST-EN-LIGNE-PLAN.png`](./docs/bloc2-test/BLOC2-TEST-EN-LIGNE-PLAN.png).
Les identifiants de statuts sont fixés dans `data.sql` et repris dans `Statuts.java` et `frontend/src/services/api.js`.
**Historique (traçabilité)** : chaque action sur une candidature ajoute une ligne, jamais modifiée, avec son auteur, sa date, l'ancien et le nouveau statut. Sont tracés : dépôt, présélection, note saisie ou corrigée, décision du test, entretien planifié ou annulé, résultat. Bouton **Historique** sur chaque carte du kanban (RH uniquement). Les candidatures antérieures ne gardent que leur dépôt, marqué « reprise des données ».
Documentation : [`docs/bloc2-test/BLOC2-HISTORIQUE.md`](./docs/bloc2-test/BLOC2-HISTORIQUE.md). Plan : [`docs/bloc2-test/BLOC2-HISTORIQUE-PLAN.png`](./docs/bloc2-test/BLOC2-HISTORIQUE-PLAN.png).

## Bloc 5 : dossier administratif du salarié

Menu **Dossiers salariés** (rôle RH uniquement) : identité, situation professionnelle, contrats et salaire, frise de carrière, pièces administratives avec leur validité, et journal d'audit de chaque consultation ou modification.
Documentation complète : [`docs/BLOC5-DOSSIER-SALARIE.md`](./docs/BLOC5-DOSSIER-SALARIE.md). Plan : [`docs/BLOC5-PLAN.png`](./docs/BLOC5-PLAN.png).

## Bloc 6 : temps de travail et pointage

**Pointage par le salarié** : le salarié active lui-même son compte (lien « Salarié : activer mon compte » sur la page de connexion, avec son matricule et l'email de son dossier), puis pointe son arrivée et son départ depuis **Mon pointage**. L'heure enregistrée est celle du serveur, une seule arrivée et un seul départ par jour.
Menu **Temps de travail** (rôle RH uniquement) : régimes et planning, contrôle des pointages (source « Salarié », « Ajouté par le RH » ou « Corrigé par le RH » ; tout ajout ou correction exige un motif), calcul du temps travaillé, détection des anomalies, compensation hebdomadaire des retards par les heures supplémentaires validées, et variables de paie.
Documentation complète : [`docs/BLOC6-TEMPS-DE-TRAVAIL.md`](./docs/BLOC6-TEMPS-DE-TRAVAIL.md). Plan : [`docs/BLOC6-PLAN.png`](./docs/BLOC6-PLAN.png).

## Bloc 7 : congés et absences

Chaque salarié a son **espace** : il consulte son solde et demande ses congés. Le **manager** de son département donne son avis, puis le **RH** décide (menu **Congés**). Le RH qualifie aussi les absences, gère les soldes, indemnise les jours non pris (salaire brut × jours / 30) et crée les comptes salariés. Congés et absences alimentent le planning et les variables de paie du Bloc 6.
Documentation complète : [`docs/BLOC7-CONGES-ABSENCES.md`](./docs/BLOC7-CONGES-ABSENCES.md). Plan : [`docs/BLOC7-PLAN.png`](./docs/BLOC7-PLAN.png).

Le menu **Congés** propose aussi un **calendrier** : une ligne par salarié, avec congés, demandes en attente, absences et présents du jour. Le RH voit tout le détail ; le manager voit seulement son département et le mot « Absent ».
Documentation : [`docs/BLOC7-CALENDRIER.md`](./docs/BLOC7-CALENDRIER.md). Plan : [`docs/BLOC7-CALENDRIER-PLAN.png`](./docs/BLOC7-CALENDRIER-PLAN.png).
