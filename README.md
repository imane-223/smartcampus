# SmartCampus

Plateforme numérique de centralisation des services universitaires
(ENSA Khouribga, projet DevOps).

## Stack technique

- Java 21, Spring Boot, Maven
- Tests : JUnit 5, AssertJ
- CI : Jenkins
- Versionnement : Git / GitHub

## Lancer le projet

    ./mvnw clean package
    java -jar target/smartcampus-0.0.1-SNAPSHOT.jar

L'application demarre sur le port 8081.
Etat de sante : http://localhost:8081/actuator/health