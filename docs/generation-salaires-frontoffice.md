# Documentation - Generation groupée des salaires FrontOffice

## Objectif

Cette fonctionnalite permet de generer le meme salaire pour plusieurs salaries en une seule action depuis le FrontOffice.

Flux respecte :

```text
React -> Spring Boot -> Dolibarr
```

Important :
- les salaries viennent de Dolibarr ;
- les salaires sont crees dans Dolibarr ;
- React n'appelle jamais Dolibarr directement ;
- Spring Boot reste le proxy unique vers l'API Dolibarr.

## Page React

Route ajoutee :

```text
/frontoffice/generation-salaires
```

Fichier :

```text
dolibarr-newapp/frontend/src/pages/GenerationSalairesPage.jsx
```

La page contient :
- formulaire de filtres ;
- tableau des salaries ;
- cases a cocher ;
- bouton `Tout selectionner` ;
- bouton `Tout deselectionner` ;
- formulaire salaire ;
- bouton `Generer salaire` ;
- message succes/erreur ;
- resume de generation.

## Filtres salariés

Filtres disponibles :

```text
Poste
Genre
Heures minimum
Heures maximum
```

Mapping Dolibarr :

```text
Poste -> job
Genre -> gender
Heure de travail -> weeklyhours
```

Le frontend appelle :

```text
GET /api/frontoffice/employes
```

Avec parametres optionnels :

```text
poste
genre
heureMin
heureMax
```

Exemple :

```http
GET /api/frontoffice/employes?poste=Commercial&genre=homme&heureMin=30&heureMax=40
```

## Generation des salaires

Le frontend envoie les salaries selectionnes au backend :

```http
POST /api/frontoffice/salaires/generer
```

Body :

```json
{
  "employeIds": [12, 13, 14],
  "dateDebut": "2026-07-01",
  "dateFin": "2026-07-31",
  "montant": 250000
}
```

Reponse :

```json
{
  "success": true,
  "message": "Salaires generes",
  "resume": {
    "salairesCrees": 3,
    "salairesIgnores": 0,
    "erreurs": []
  }
}
```

## Backend ajouté

Controllers :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/controller/FrontofficeEmployeController.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/controller/FrontofficeSalaireController.java
```

Services :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/service/EmployeService.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/service/SalaireGenerationService.java
```

DTO :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/GenererSalairesRequest.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/GenerationSalairesResponse.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/ResumeGenerationSalairesDto.java
```

DTO modifie :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/EmployeListeDto.java
```

Champs ajoutes dans `EmployeListeDto` :

```text
refEmploye
poste
actif
```

## Frontend ajouté

Page :

```text
dolibarr-newapp/frontend/src/pages/GenerationSalairesPage.jsx
```

Services :

```text
dolibarr-newapp/frontend/src/services/employeService.js
dolibarr-newapp/frontend/src/services/salaireService.js
```

Fichiers modifies :

```text
dolibarr-newapp/frontend/src/App.jsx
dolibarr-newapp/frontend/src/components/FrontOfficeLayout.jsx
dolibarr-newapp/frontend/src/styles.css
```

## Services backend

### EmployeService

Fonctions principales :

```text
rechercherEmployes()
filtrerParPoste()
filtrerParGenre()
filtrerParHeures()
```

Role :
- lire les users Dolibarr via `DolibarrClientService` ;
- exclure le superadmin ;
- filtrer par poste, genre et heures ;
- renvoyer une liste exploitable par React.

### SalaireGenerationService

Fonctions principales :

```text
genererSalaires()
creerSalairePourEmploye()
creerResumeGeneration()
ajouterErreur()
```

Role :
- recevoir la liste des employes selectionnes ;
- verifier chaque employe ;
- ignorer les employes introuvables ;
- ignorer les employes inactifs ;
- verifier les doublons de salaire pour la meme periode ;
- creer les salaires valides dans Dolibarr ;
- continuer le traitement meme si un employe echoue.

## Regles métier

Les salaires sont generes uniquement pour les employes selectionnes.

Un employe est ignore si :
- il est introuvable ;
- il est inactif ;
- il est invalide ;
- un salaire existe deja pour le meme employe, la meme date de debut et la meme date de fin.

Si une erreur arrive sur un employe :
- elle est ajoutee au resume ;
- les autres employes continuent d'etre traites.

Les salaires generes sont marques avec :

```text
NEWAPP_GENERATION
```

Champs envoyes a Dolibarr si acceptes par l'API :

```text
import_key = NEWAPP
note_private = NEWAPP_GENERATION
ref = NEWAPP_GENERATION-{employeId}-{dateDebut}-{dateFin}
```

## Validation frontend

La page React verifie :

```text
dateDebut obligatoire
dateFin obligatoire
montant obligatoire
montant > 0
au moins un salarie selectionne
dateFin >= dateDebut
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

Ouvrir :

```text
http://localhost:5173/frontoffice/generation-salaires
```

## Test API avec curl

Lister les employes filtres :

```bash
curl "http://localhost:8080/api/frontoffice/employes?poste=Commercial&genre=homme&heureMin=30&heureMax=40"
```

Generer les salaires :

```bash
curl -X POST http://localhost:8080/api/frontoffice/salaires/generer ^
  -H "Content-Type: application/json" ^
  -d "{\"employeIds\":[12,13,14],\"dateDebut\":\"2026-07-01\",\"dateFin\":\"2026-07-31\",\"montant\":250000}"
```

## Verification dans Dolibarr

Apres generation :

1. Aller dans Dolibarr.
2. Ouvrir le module salaires.
3. Verifier que les salaires ont ete crees pour les employes selectionnes.
4. Verifier la periode :

```text
dateDebut = 2026-07-01
dateFin = 2026-07-31
```

5. Verifier le montant.
6. Verifier si possible la trace :

```text
NEWAPP_GENERATION
```

## Points importants

Cette fonctionnalite ne modifie pas :
- le Backoffice ;
- l'import CSV ;
- le reset ;
- le dashboard ;
- les jours feries SQLite ;
- la connexion Dolibarr existante.

Elle ajoute seulement un nouveau flux FrontOffice pour creer plusieurs salaires Dolibarr en une action.
