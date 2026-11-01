# SmartCampus

Plateforme numérique de centralisation des services universitaires
(ENSA Khouribga, projet DevOps).

## Stack technique

- Java 21, Spring Boot 4.1, Maven
- Spring Data JPA, Flyway, PostgreSQL 16
- Tests : JUnit 5, AssertJ, H2 en mémoire
- CI : Jenkins
- Versionnement : Git / GitHub

## Lancer le projet

L'application exige désormais une base de données pour démarrer. Sans base,
elle s'arrête au démarrage.

    docker compose up -d db
    ./mvnw spring-boot:run

L'application démarre sur le port 8081. Flyway crée ou met à jour le schéma à
chaque démarrage.

État de santé : http://localhost:8081/actuator/health

Connexion à la base : variables `DB_URL`, `DB_USER` et `DB_PASSWORD`. Sans ces
variables, l'application utilise les valeurs de développement du `compose.yaml`
(`localhost:5432`, base, utilisateur et mot de passe `smartcampus`).

Le port 5432 doit être libre : arrêtez un éventuel PostgreSQL installé sur le poste.

### Avec Docker

`docker run` seul ne suffit plus, car le conteneur de l'application ne trouve
aucune base. Il faut lancer l'ensemble avec Compose :

    ./mvnw clean package
    docker compose up -d --build

L'API est alors accessible sur http://localhost:8090.

## Tests

    ./mvnw clean package

Les tests n'ont pas besoin de PostgreSQL. Ils utilisent le profil `test` : une base
H2 en mémoire en mode PostgreSQL, sur laquelle Flyway exécute les vraies migrations,
puis Hibernate vérifie les entités (`ddl-auto=validate`).

**Limite connue :** H2 n'est pas PostgreSQL. Une migration qui utilise une
fonction propre à PostgreSQL passerait mal sur H2, et un comportement propre à
PostgreSQL n'est pas testé. Le bon outil est Testcontainers (tests sur un vrai
PostgreSQL 16 dans Docker). Il fera l'objet d'un ticket ultérieur. En attendant,
les migrations doivent rester en SQL portable : voir
[docs/conventions.md](docs/conventions.md).

## Conventions d'équipe

Organisation des packages, migrations, nommage, DTO, erreurs : voir
[docs/conventions.md](docs/conventions.md).
