# Documentation - Jours feries SQLite

## Objectif

Cette modification ajoute une gestion locale des jours feries dans NewApp.

Important :
- les employes, salaires et paiements restent dans Dolibarr ;
- les jours feries sont stockes uniquement dans SQLite ;
- React appelle Spring Boot ;
- React ne doit jamais appeler Dolibarr directement.

## Fichier SQLite

La base SQLite est configuree dans :

```text
dolibarr-newapp/backend/src/main/resources/application.properties
```

Configuration ajoutee :

```properties
spring.datasource.url=jdbc:sqlite:newapp.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update
```

Le fichier de base est cree au lancement du backend :

```text
dolibarr-newapp/backend/newapp.db
```

## Table SQLite

La table geree par JPA est :

```sql
jour_ferie
```

Colonnes :

```sql
id INTEGER PRIMARY KEY AUTOINCREMENT
nom TEXT NOT NULL
date_jour DATE NOT NULL
description TEXT
actif BOOLEAN DEFAULT true
```

L'entite Java correspondante est :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/model/JourFerie.java
```

## Backend ajoute

Fichiers ajoutes :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/model/JourFerie.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/JourFerieDto.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/repository/JourFerieRepository.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/service/JourFerieService.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/controller/JourFerieController.java
```

Responsabilites :

- `JourFerieController` expose les endpoints REST.
- `JourFerieService` contient la logique metier.
- `JourFerieRepository` fait les operations SQLite via Spring Data JPA.
- `JourFerie` mappe la table `jour_ferie`.
- `JourFerieDto` represente le JSON envoye/recu par React.

## Ou se fait l'insertion SQLite ?

L'insertion SQLite des jours feries ne vient pas de l'import CSV employes/salaires.

Elle se fait dans le CRUD backoffice, via :

```text
JourFerieController.java
POST /api/backoffice/jours-feries
```

Puis le controller appelle :

```java
jourFerieService.creer(dto)
```

Dans :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/service/JourFerieService.java
```

La sauvegarde effective en SQLite est cette ligne :

```java
return versDto(jourFerieRepository.save(jourFerie));
```

`jourFerieRepository.save(...)` fait l'INSERT dans SQLite si l'entite n'a pas encore d'id.

## Il n'y a pas d'import CSV jours feries

Actuellement :

- l'import CSV employes envoie les employes vers Dolibarr ;
- l'import CSV salaires envoie les salaires/paiements vers Dolibarr ;
- les jours feries ne sont pas importes depuis CSV ;
- les jours feries sont crees, modifies et supprimes depuis la page backoffice React.

Si un import CSV jours feries est demande plus tard, il faudra ajouter un flux separe :

```text
CSV jours_feries -> React -> Spring Boot -> SQLite
```

Sans passer par Dolibarr.

## Endpoints jours feries

Lister :

```http
GET /api/backoffice/jours-feries
```

Voir un jour ferie :

```http
GET /api/backoffice/jours-feries/{id}
```

Creer :

```http
POST /api/backoffice/jours-feries
X-BACKOFFICE-CODE: BACKOFFICE-2026
Content-Type: application/json
```

Modifier :

```http
PUT /api/backoffice/jours-feries/{id}
X-BACKOFFICE-CODE: BACKOFFICE-2026
Content-Type: application/json
```

Supprimer :

```http
DELETE /api/backoffice/jours-feries/{id}
X-BACKOFFICE-CODE: BACKOFFICE-2026
```

Body creation/modification :

```json
{
  "nom": "Fete nationale",
  "dateJour": "2026-06-26",
  "description": "Fete nationale Madagascar",
  "actif": true
}
```

## Frontend ajoute

Fichiers ajoutes :

```text
dolibarr-newapp/frontend/src/pages/JoursFeriesPage.jsx
dolibarr-newapp/frontend/src/services/jourFerieService.js
```

Fichiers modifies :

```text
dolibarr-newapp/frontend/src/App.jsx
dolibarr-newapp/frontend/src/components/Sidebar.jsx
dolibarr-newapp/frontend/src/styles.css
```

Route :

```text
/backoffice/jours-feries
```

La page React appelle uniquement Spring Boot via :

```text
dolibarr-newapp/frontend/src/services/jourFerieService.js
```

## Modification import employes

Le CSV employes accepte maintenant la colonne :

```text
poste
```

Mapping :

```text
poste -> job dans llx_user Dolibarr
```

Fichiers modifies :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/EmployeImportDto.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/service/DolibarrImportService.java
dolibarr-newapp/frontend/src/utils/analyseCsv.js
dolibarr-newapp/frontend/src/pages/PageImport.jsx
```

Regles conservees :

```text
login = newapp_ + identifiant
employee = 1
statut = 1
import_key = NEWAPP
note_private contient NEWAPP_IMPORT
```

## Verifier dans SQLite

Lancer le backend :

```bash
cd dolibarr-newapp/backend
mvn spring-boot:run
```

Ouvrir la base :

```bash
cd dolibarr-newapp/backend
sqlite3 newapp.db
```

Commandes SQLite :

```sql
.tables
.schema jour_ferie
SELECT * FROM jour_ferie;
```

Insertion manuelle de test :

```sql
INSERT INTO jour_ferie (nom, date_jour, description, actif)
VALUES ('Fete nationale', '2026-06-26', 'Fete nationale Madagascar', 1);
```

Quitter :

```sql
.quit
```

## Commandes de test

Backend :

```bash
cd dolibarr-newapp/backend
mvn test
mvn spring-boot:run
```

Frontend :

```bash
cd dolibarr-newapp/frontend
cmd /c npm run build
cmd /c npm run dev
```

## Exemple curl

Creer un jour ferie :

```bash
curl -X POST http://localhost:8080/api/backoffice/jours-feries ^
  -H "Content-Type: application/json" ^
  -H "X-BACKOFFICE-CODE: BACKOFFICE-2026" ^
  -d "{\"nom\":\"Fete nationale\",\"dateJour\":\"2026-06-26\",\"description\":\"Fete nationale Madagascar\",\"actif\":true}"
```

Lister :

```bash
curl http://localhost:8080/api/backoffice/jours-feries
```

Supprimer :

```bash
curl -X DELETE http://localhost:8080/api/backoffice/jours-feries/1 ^
  -H "X-BACKOFFICE-CODE: BACKOFFICE-2026"
```
