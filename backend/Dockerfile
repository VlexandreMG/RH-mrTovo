# ==========================================
# Étape 1 : Compilation du backend Spring Boot
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Mise en cache des dépendances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copie du code source et compilation du JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Étape 2 : Image d'exécution légère (JRE 21)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Utilisateur non-root pour la sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copie du JAR produit par la phase de compilation
COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
