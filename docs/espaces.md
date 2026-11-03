# Module Espaces

## Démarrage local

Prérequis : JDK 21, Maven, Node.js 22 et npm. PostgreSQL 16 peut être lancé avec Docker Compose :

```powershell
docker compose up -d db
mvn spring-boot:run
```

Flyway applique automatiquement `V2__espaces.sql` au démarrage. Pour la base locale, les valeurs par défaut sont `smartcampus` / `smartcampus` sur `localhost:5433`. Les variables `DB_URL`, `DB_USER` et `DB_PASSWORD` permettent de les remplacer.

Dans un autre terminal :

```powershell
npm --prefix frontend ci
npm --prefix frontend run dev
```

Vite sert l’application sur `http://localhost:5173` et transmet `/api` au backend `http://localhost:8081`. Le frontend utilise les endpoints de session existants, les cookies `JSESSIONID` et le jeton CSRF `X-XSRF-TOKEN`.

## Modèle et règles métier

- `Espace` est associé à un `Batiment` minimal. Le module expose la liste des bâtiments et permet à un administrateur d’en ajouter un pour alimenter les formulaires.
- Les codes d’espace sont supprimés des espaces extérieurs, convertis en majuscules et uniques. La règle est contrôlée dans le service et par la contrainte SQL.
- Nom, type, bâtiment et capacité sont obligatoires ; la capacité est strictement positive.
- Les types possibles sont `CLASSROOM`, `COMPUTER_LAB`, `MEETING_ROOM`, `AMPHITHEATER`, `STUDY_ROOM` et `OTHER`.
- Les statuts possibles sont `ACTIVE` et `MAINTENANCE`. La maintenance reste visible, mais `reservable` vaut `false`.
- L’archivage est distinct du statut. Les espaces archivés sont exclus du catalogue et du détail standard, conservés en base, et peuvent être restaurés via un `PUT` administrateur.
- `EspaceService` expose les méthodes métier `existe`, `statutDe`, `estArchive` et `estReservable` pour le futur module Réservations. Le module Réservations ne doit pas accéder directement aux repositories `room`.

## API

Toutes les routes sont sous `/api/salles`. Les consultations nécessitent une session authentifiée. Les créations et modifications sont réservées au rôle `ADMIN` et requièrent le jeton CSRF déjà utilisé par le backend. Les erreurs suivent le format partagé `ErrorResponse`.

| Méthode | Route | Accès | Résultat |
|---|---|---|---|
| `GET` | `/api/salles` | Authentifié | Catalogue paginé ; filtres facultatifs `type`, `batimentId`, `statut`, `search` |
| `GET` | `/api/salles/{id}` | Authentifié | Détail d’un espace non archivé ; 404 si absent ou archivé |
| `POST` | `/api/salles` | ADMIN | Crée un espace ; 201, 400, 404 ou 409 |
| `PUT` | `/api/salles/{id}` | ADMIN | Remplacement complet des propriétés fournies ; 200, 400, 404 ou 409 |
| `GET` | `/api/salles/batiments` | Authentifié | Référentiel des bâtiments disponibles |
| `POST` | `/api/salles/batiments` | ADMIN | Ajoute un bâtiment de référence ; 201 ou 409 |

Pagination : `page` commence à zéro, `size` vaut 20 par défaut, et le tri par défaut est `nom,asc`. Exemple :

```text
GET /api/salles?type=CLASSROOM&batimentId=1&statut=ACTIVE&search=A101&page=0&size=20
```

Le JSON utilise les noms métier français en camelCase, par exemple :

```json
{
  "code": "SAL-A101",
  "nom": "Salle A101",
  "type": "CLASSROOM",
  "capacite": 40,
  "batimentId": 1,
  "etage": 1,
  "description": "Salle de cours équipée",
  "statut": "ACTIVE"
}
```

Pour un `PUT`, `code`, `nom`, `type`, `capacite`, `batimentId`, `statut` et `archive` sont requis. `etage` et `description` peuvent être `null` pour les effacer. Aucun identifiant n’est modifiable.

## Vérifications

```powershell
mvn clean test
npm --prefix frontend run build
npm --prefix frontend test
```

Les tests backend utilisent H2 en mode PostgreSQL et exécutent les migrations Flyway. Ils ne remplacent pas un test sur PostgreSQL réel/Testcontainers.
