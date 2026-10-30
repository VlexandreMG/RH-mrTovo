# ==========================================
# Étape 1 : Compilation du frontend Vue.js
# ==========================================
FROM node:22-alpine AS build
WORKDIR /app

# Variable d'environnement pour l'API (modifiable via build-arg)
ARG VITE_API_BASE_URL=http://localhost:8080/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Installation des dépendances
COPY package*.json ./
RUN npm install

# Copie du code source et compilation pour la production
COPY . .
RUN npm run build

# ==========================================
# Étape 2 : Serveur web de production Nginx
# ==========================================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Nettoyage du répertoire par défaut de nginx
RUN rm -rf ./*

# Copie des fichiers compilés depuis l'étape de build
COPY --from=build /app/dist .

# Copie de la configuration Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
