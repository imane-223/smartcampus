# SmartCampus

Plateforme numérique de centralisation des services universitaires
(ENSA Khouribga, projet DevOps).

## Stack technique

- Java 21, Spring Boot 4.1, Maven
- Spring Security 7 (session serveur, CSRF), Spring Data JPA, Flyway, PostgreSQL 16
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

## Comptes de démonstration

Données fictives, créées au démarrage **uniquement** si la propriété
`smartcampus.demo-data.enabled` vaut `true` (variable d'environnement
`SMARTCAMPUS_DEMO_DATA_ENABLED`). Elle est **désactivée par défaut**, et ne doit
jamais être activée en production : les mots de passe ci-dessous sont publics.

- Le `compose.yaml` l'active pour la pile Docker locale.
- En local, sans Docker : `SMARTCAMPUS_DEMO_DATA_ENABLED=true ./mvnw spring-boot:run`.
- L'initialisation est idempotente : un compte déjà présent n'est ni recréé ni modifié.

| Rôle | E-mail | Mot de passe | État |
|---|---|---|---|
| Étudiant | `etudiant@usms.ac.ma` | `Demo2026!` | actif |
| Enseignant | `prof@usms.ma` | `Demo2026!` | actif |
| Administrateur | `admin@ensakh.ma` | `Demo2026!` | actif |
| Étudiant | `ancien@usms.ac.ma` | `Demo2026!` | désactivé |

## Authentification (API)

Session serveur et cookie `JSESSIONID`, protégés par un jeton CSRF (cookie
`XSRF-TOKEN`, renvoyé dans l'en-tête `X-XSRF-TOKEN` sur chaque POST, PUT, PATCH et DELETE).

| Méthode | URL | Accès | Réponse |
|---|---|---|---|
| GET | `/api/auth/csrf` | public | 200, pose le cookie `XSRF-TOKEN` |
| POST | `/api/auth/login` | public + CSRF | 200 utilisateur, 401 `ErrorResponse`, 400 si champ vide |
| GET | `/api/auth/me` | connecté | 200 utilisateur, sinon 401 |
| GET | `/actuator/health` | public | 200 `UP` (healthcheck Docker) |

Après 5 échecs consécutifs, un compte est verrouillé 15 minutes. Le message
`ACCOUNT_LOCKED` n'apparaît que si le mot de passe saisi est correct, comme pour un
compte désactivé : sinon, la réponse reste « E-mail ou mot de passe incorrect. ».

Exemple avec curl, sur la pile Docker (port 8090) :

    curl -c jar -b jar http://localhost:8090/api/auth/csrf
    TOKEN=$(grep XSRF-TOKEN jar | awk '{print $7}')
    curl -c jar -b jar -H "X-XSRF-TOKEN: $TOKEN" -H "Content-Type: application/json" \
         -d '{"courriel":"etudiant@usms.ac.ma","motDePasse":"Demo2026!"}' \
         http://localhost:8090/api/auth/login
    curl -b jar http://localhost:8090/api/auth/me

Le jeton CSRF change après la connexion : relisez-le dans `jar` avant le POST suivant.

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
