# Documentation - Liste salaries FrontOffice

## Objectif

Cette fonctionnalite ajoute une nouvelle zone FrontOffice pour consulter les salaries NewApp venant de Dolibarr, puis voir le detail d'un salarie avec son historique de salaires et de paiements.

Flux respecte :

```text
React -> Spring Boot -> Dolibarr
```

React ne contacte jamais Dolibarr directement.

## Routes React

Routes ajoutees :

```text
/frontoffice/salaries
/frontoffice/salaries/:id
```

Page liste :

```text
dolibarr-newapp/frontend/src/pages/ListeSalariesPage.jsx
```

Page detail :

```text
dolibarr-newapp/frontend/src/pages/DetailSalariePage.jsx
```

Service frontend :

```text
dolibarr-newapp/frontend/src/services/salarieService.js
```

Routes declarees dans :

```text
dolibarr-newapp/frontend/src/App.jsx
```

Lien ajoute dans :

```text
dolibarr-newapp/frontend/src/components/FrontOfficeLayout.jsx
```

Styles ajoutes dans :

```text
dolibarr-newapp/frontend/src/styles.css
```

## Endpoints Spring Boot

### Liste des salaries

```http
GET /api/frontoffice/salaries
```

Exemple de reponse :

```json
[
  {
    "id": 12,
    "refEmploye": "1",
    "nom": "Rakotobe",
    "prenom": "",
    "identifiant": "newapp_rakoto1",
    "poste": "Commercial",
    "genre": "homme",
    "heureTravailSemaine": 35,
    "statut": "Actif"
  }
]
```

### Detail d'un salarie

```http
GET /api/frontoffice/salaries/{id}
```

Exemple de reponse :

```json
{
  "salarie": {
    "id": 12,
    "refEmploye": "1",
    "nom": "Rakotobe",
    "prenom": "",
    "identifiant": "newapp_rakoto1",
    "poste": "Commercial",
    "genre": "homme",
    "heureTravailSemaine": 35,
    "statut": "Actif"
  },
  "historiquesSalaires": [
    {
      "idSalaire": 101,
      "refSalaire": "SAL-001",
      "dateDebut": "2026-03-01",
      "dateFin": "2026-03-08",
      "montantSalaire": 890,
      "totalPaye": 480,
      "resteAPayer": 410,
      "statutPaiement": "Partiel",
      "paiements": [
        {
          "idPaiement": 201,
          "datePaiement": "2026-03-08",
          "montantPaiement": 480,
          "referencePaiement": "PAY-001"
        }
      ]
    }
  ]
}
```

## Backend ajoute

Controller :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/controller/SalarieFrontofficeController.java
```

Service :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/service/SalarieFrontofficeService.java
```

DTO :

```text
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/SalarieFrontofficeDto.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/DetailSalarieFrontofficeDto.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/SalaireHistoriqueDto.java
dolibarr-newapp/backend/src/main/java/com/newapp/dolibarr/dto/PaiementHistoriqueDto.java
```

## Fonctions backend

Dans `SalarieFrontofficeService` :

```text
recupererTousLesSalaries()
recupererDetailSalarie()
recupererSalairesDuSalarie()
recupererPaiementsDuSalaire()
calculerTotalPaye()
calculerResteAPayer()
determinerStatutPaiement()
convertirSalarie()
convertirSalaire()
convertirPaiement()
```

## Regles metier

La liste affiche seulement les salaries affichables :

- utilisateur actif si possible ;
- utilisateur NewApp si possible ;
- login commence par `newapp_`, ou `import_key = NEWAPP`, ou `note_private` contient `NEWAPP_IMPORT` ;
- superadmin Dolibarr exclu ;
- admins Dolibarr exclus si le champ admin est disponible.

Si un salarie n'a aucun salaire :

```json
"historiquesSalaires": []
```

Si un salaire n'a aucun paiement :

```text
totalPaye = 0
resteAPayer = montantSalaire
statutPaiement = Non paye
```

Calculs :

```text
resteAPayer = montantSalaire - totalPaye
```

Statut paiement :

```text
Paye     si resteAPayer <= 0
Partiel  si totalPaye > 0 et resteAPayer > 0
Non paye si totalPaye = 0
```

Les doublons de paiements sont evites par `idPaiement` quand disponible.

## Frontend

### ListeSalariesPage

Fonction :

```text
chargerSalaries()
```

Affiche :

- ID ;
- reference employe ;
- nom ;
- identifiant ;
- poste ;
- genre ;
- heures de travail ;
- action `Voir detail`.

### DetailSalariePage

Fonctions :

```text
chargerDetailSalarie()
afficherStatutPaiement()
formaterMontant()
formaterDate()
retournerListe()
```

Affiche :

- infos du salarie ;
- historique des salaires ;
- paiements de chaque salaire ;
- reste a payer clairement ;
- bouton retour liste.

## Test backend

Lancer le backend :

```bash
cd dolibarr-newapp/backend
mvn spring-boot:run
```

Tester la liste :

```bash
curl http://localhost:8080/api/frontoffice/salaries
```

Tester le detail :

```bash
curl http://localhost:8080/api/frontoffice/salaries/12
```

## Test frontend

Lancer le frontend :

```bash
cd dolibarr-newapp/frontend
cmd /c npm run dev
```

Ouvrir :

```text
http://localhost:5173/frontoffice/salaries
```

Puis cliquer sur :

```text
Voir detail
```

La page doit rediriger vers :

```text
http://localhost:5173/frontoffice/salaries/{id}
```

## Verification

Commandes executees :

```bash
cd dolibarr-newapp/backend
mvn test
```

```bash
cd dolibarr-newapp/frontend
cmd /c npm run build
```

Resultat :

```text
Backend compile OK
Frontend build OK
Spring Boot demarre OK avec les nouveaux endpoints
```

## Non regression

Cette fonctionnalite ne modifie pas les flux suivants :

- Backoffice ;
- import CSV ;
- reset ;
- dashboard ;
- jours feries SQLite ;
- generation groupée des salaires ;
- connexion Dolibarr.
