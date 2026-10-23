# Rapport d'arrangement du code

**Date :** vendredi 23 octobre 2026
**Heure :** 11:52 (heure de Madagascar, UTC+03:00)
**Point de départ :** commit `77cf276` « Merge branch 'bloc2-bis' »

## 1. Problème constaté

Le merge de `bloc2-bis` (Bloc 2 : offres / candidatures / kanban) dans `main` (Bloc 1 : demandes de recrutement, authentification) a été commité **avec des conflits non résolus**. Le projet ne compilait plus :

| Fichier | Problème |
|---|---|
| `backend/pom.xml` | Marqueurs `<<<<<<<` / `>>>>>>>` : Maven refusait de lire le POM |
| `backend/.../application.properties` | Marqueurs de conflit (résolus en local, mais Bloc 2 non pris en compte) |
| `frontend/index.html`, `src/App.vue`, `src/main.js`, `src/router/index.js`, `src/assets/main.css` | Marqueurs de conflit : le frontend ne démarrait plus |
| `frontend/package.json` | Remplacé par celui du Bloc 2 : **`pinia` avait disparu** (le Bloc 1 en dépend) |

D'autres incohérences venaient du rapprochement des deux blocs :

- Le Bloc 2 appelait l'API sur le port **8080** sans authentification, alors que le backend écoute sur **8012** et exige une connexion (`SecurityConfig`).
- Deux `@RestControllerAdvice` (`GlobalExceptionHandler` et `GestionErreurs`) géraient **la même exception** (`HttpMessageNotReadableException`).
- Les tables du Bloc 2 (schéma `mrtovo`) n'étaient plus créées : `schema.sql` / `data.sql` ne s'exécutaient plus.
- `SET search_path TO mrtovo` restait actif sur la connexion du pool, ce qui pouvait faire chercher les tables du Bloc 1 dans le mauvais schéma.
- Les styles globaux du Bloc 2 (`button`, `input`, `body`) écrasaient ceux du Bloc 1.

## 2. Corrections apportées

### Backend

| Fichier | Modification |
|---|---|
| `pom.xml` | Conflit résolu : on garde `spring-security-test` (validation et lombok étaient déjà déclarés) |
| `application.properties` | Ajout de `spring.sql.init.mode=always` : le Bloc 2 est recréé à chaque démarrage, Hibernate (`update`) gère toujours le Bloc 1. Vos identifiants de base locaux sont conservés |
| `schema.sql`, `data.sql` | Ajout de `RESET search_path;` à la fin de chaque script |
| `exception/GestionErreurs.java` | Suppression du handler en double ; ce fichier ne gère plus que les erreurs du Bloc 2 (409, 404, validation) |

### Frontend

| Fichier | Modification |
|---|---|
| `package.json` | Nom `frontend`, `pinia` restauré, `axios` retiré (plus utilisé) ; `package-lock.json` régénéré par `npm install` |
| `index.html`, `App.vue`, `main.js` | Version Bloc 1 (polices, Pinia, `ToastHost`) |
| `router/index.js` | Routes du Bloc 2 (`/offres`, `/offres/:id/kanban`) ajoutées **dans la mise en page authentifiée**, réservées au rôle RH |
| `components/AppLayout.vue` | Nouvelle entrée de menu **Candidatures** (rôle RH) |
| `services/api.js` | Réécrit sur le client commun `api/http.js` : bon port (8012), en-tête d'authentification, redirection vers la connexion sur 401 |
| `api/http.js` | Ajout de `apiPut` (utilisé pour la saisie de la note) |
| `assets/main.css` | Charte du Bloc 1 conservée ; styles du Bloc 2 limités au conteneur `.recrutement` |
| `OffreList.vue`, `KanbanBoard.vue` | Contenu placé dans `<div class="recrutement">` |
| `vite.config.js` | Commentaire corrigé (le CORS est configuré dans `SecurityConfig.java`) |

### Documentation

- `README.md` : explication des deux schémas (`public` / `mrtovo`), vrais comptes de démonstration (un manager par département), section Bloc 2.

## 3. Vérifications

| Test | Résultat |
|---|---|
| Recherche de marqueurs de conflit dans tout le dépôt | Aucun |
| `./mvnw compile` | OK |
| `npm run build` | OK |
| Démarrage du backend (port de test 8099) | OK, démarré en 6,6 s |
| `POST /api/auth/login` (rh@rh.com) | OK |
| `GET /api/offres` (Bloc 2, connecté) | OK : 1 offre, 7 candidatures |
| `GET /api/referentiels/postes` (Bloc 1) | OK |
| `GET /api/offres` sans connexion | 401 (attendu) |
| Corps invalide / champ manquant | Messages d'erreur lisibles (400) |

Les interfaces n'ont pas été testées dans un navigateur.

## 4. Reste à faire (à la main)

Ces actions suppriment des fichiers. Elles ont été bloquées par les permissions et sont donc laissées à votre choix :

```bash
# 1. Doublon de configuration CORS (SecurityConfig s'en occupe déjà)
git rm backend/src/main/java/com/mrtovo/backend/config/CorsConfig.java

# 2. Cache Vite d'un ancien projet, commité par erreur (déjà dans .gitignore)
git rm -r --cached frontend-v2

# 3. (Optionnel) Restaurer les outils de lint/format du Bloc 1, supprimés par le merge
git checkout 3c8844c -- frontend/eslint.config.js frontend/.oxlintrc.json frontend/.prettierrc.json \
  frontend/.editorconfig frontend/.gitattributes frontend/jsconfig.json
#    puis rajouter les scripts "lint" / "format" et leurs devDependencies dans package.json
```

Autres points à décider en équipe :

- **Lien Bloc 1 → Bloc 2** : une demande au statut `RECRUTEMENT_OUVERT` ne crée pas encore d'`offre_emploi`. Les offres viennent seulement de `data.sql`.
- **Droits backend du Bloc 2** : seul le menu est réservé au RH. Les endpoints `/api/offres`, `/api/candidatures` et `/api/entretiens` restent ouverts à tout utilisateur connecté (ajouter `@PreAuthorize("hasRole('RH')")` si besoin).
- **`application.properties`** contient vos identifiants locaux (`mrtovo/mrtovo`, port 5432) au lieu de ceux de l'équipe (`postgres/1234`, port 5433). Pensez-y avant de commiter.
- **`TestController`** (`/api/hello`) : code de démarrage qui ne sert plus à rien.

Aucun commit n'a été fait : toutes les modifications sont dans l'arbre de travail (`git diff` pour les relire).
