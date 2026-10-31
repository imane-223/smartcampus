# Image de base : un JRE 21 suffit pour executer le JAR (pas besoin du JDK)
FROM eclipse-temurin:21-jre-alpine

# Dossier de travail dans le conteneur
WORKDIR /app

# Dossier des donnees persistantes (monte par un volume)
RUN mkdir -p /app/data

# Copie du JAR produit par Maven ou par Jenkins
COPY target/smartcampus-0.0.1-SNAPSHOT.jar app.jar

# Port utilise par l'application a l'interieur du conteneur
EXPOSE 8081

# Commande lancee au demarrage
ENTRYPOINT ["java", "-jar", "/app/app.jar"]