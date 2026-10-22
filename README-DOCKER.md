# 🐳 Guide Docker & Docker Compose - RH Mr Tovo

Ce projet utilise Docker et Docker Compose pour compiler et exécuter l'ensemble de la pile technique (Base de données PostgreSQL, Backend Spring Boot et Frontend Vue.js 3).

---

## 🏗️ Architecture des Conteneurs

| Service | Rôle | Image / Dockerfile | Port Hôte |
| :--- | :--- | :--- | :--- |
| **db** | Base de données PostgreSQL 16 | `postgres:16-alpine` | `5433` (évite conflit avec Postgres local) |
| **backend** | API REST Spring Boot (Java 21) | Multi-stage build (`backend/Dockerfile`) | `8080` |
| **frontend** | Application Vue.js 3 + Nginx | Multi-stage build (`frontend/Dockerfile`) | `5173` et `80` |

---

## 🚀 Démarrage Rapide

### 1. Prérequis
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installé et en cours d'exécution.

### 2. Compilation et lancement de tous les services
Dans le terminal à la racine du projet :

```bash
docker compose up --build -d
```

- `--build` : Force la compilation du backend (Maven) et du frontend (NPM/Vite).
- `-d` : Exécute les conteneurs en arrière-plan (mode détaché).

### 3. Accès aux applications
- **Frontend Vue.js** : [http://localhost:5173](http://localhost:5173) ou [http://localhost](http://localhost)
- **Backend API Spring Boot** : [http://localhost:8080/api](http://localhost:8080/api)
- **Base PostgreSQL** : `localhost:5433` (User: `christelle`, Base: `rh_db`)

---

## 📋 Commandes Utiles

### Voir les logs en direct
```bash
# Tous les services
docker compose logs -f

# Uniquement le backend
docker compose logs -f backend

# Uniquement le frontend
docker compose logs -f frontend

# Uniquement la base de données
docker compose logs -f db
```

### Vérifier l'état des conteneurs
```bash
docker compose ps
```

### Arrêter les conteneurs
```bash
# Arrêter les conteneurs sans supprimer les données
docker compose down

# Arrêter et supprimer aussi les volumes de données (réinitialisation complète)
docker compose down -v
```

### Recompiler un seul service après modification
```bash
# Recompiler et relancer le backend
docker compose up -d --build backend

# Recompiler et relancer le frontend
docker compose up -d --build frontend
```

---

## ⚙️ Initialisation de la Base de Données

Le script SQL situé dans [backend/sql/22_10_26_01_table.sql](file:///c:/Users/AINA%20SATAMANDRESY/Desktop/rattra_Tovo/RH-mrTovo/backend/sql/22_10_26_01_table.sql) est automatiquement exécuté au premier démarrage du conteneur PostgreSQL grâce au point de montage Docker `/docker-entrypoint-initdb.d/`.
