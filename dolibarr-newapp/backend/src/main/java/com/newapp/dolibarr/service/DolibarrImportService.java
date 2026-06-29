package com.newapp.dolibarr.service;

import com.newapp.dolibarr.config.DolibarrProperties;
import com.newapp.dolibarr.dto.EmployeImportDto;
import com.newapp.dolibarr.dto.ImportResultResponse;
import com.newapp.dolibarr.dto.LigneResultatImport;
import com.newapp.dolibarr.dto.PaiementImportDto;
import com.newapp.dolibarr.dto.PhotoImportDto;
import com.newapp.dolibarr.dto.SalaireImportDto;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Logique d'import des employés et des salaires dans Dolibarr, via le proxy
 * {@link DolibarrClientService}. L'import est tolérant aux erreurs : une ligne en
 * échec n'interrompt pas le traitement des suivantes, le détail est remonté au frontend.
 *
 * IMPORTANT (à confirmer sur le Swagger de l'instance avant mise en production) :
 *  - création employé : POST /users, champs login / password / lastname / gender / weeklyhours ;
 *  - création salaire  : POST /salaries (voir {@link #construireCorpsSalaire}) ;
 *  - création paiement : POST /salaries/{id}/payments (voir {@link #construireCorpsPaiement}).
 * Les noms de champs incertains sont isolés dans les méthodes "construireCorps*" pour
 * être ajustés sans toucher au reste de la logique.
 */
@Service
public class DolibarrImportService {

    private static final Logger logger = LoggerFactory.getLogger(DolibarrImportService.class);

    /** ID du superadmin Dolibarr : jamais créé ni modifié, garde-fou explicite. */
    private static final long ID_SUPERADMIN = 1L;

    private final DolibarrClientService dolibarrClientService;
    private final DolibarrProperties dolibarrProperties;
    private final ImportTrackingService importTrackingService;
    private final GenerateurMiniatures generateurMiniatures;

    /** ID numérique du type de règlement Dolibarr (c_paiement.id) pour les paiements de salaire. */
    private final Integer typePaiementId;
    /** ID du compte bancaire Dolibarr (obligatoire seulement si le module banque est activé ; 0 sinon). */
    private final Integer compteBancaireId;

    public DolibarrImportService(
            DolibarrClientService dolibarrClientService,
            DolibarrProperties dolibarrProperties,
            ImportTrackingService importTrackingService,
            GenerateurMiniatures generateurMiniatures,
            @Value("${import.paiement.type-paiement-id:0}") Integer typePaiementId,
            @Value("${import.paiement.compte-bancaire-id:0}") Integer compteBancaireId
    ) {
        this.dolibarrClientService = dolibarrClientService;
        this.dolibarrProperties = dolibarrProperties;
        this.importTrackingService = importTrackingService;
        this.generateurMiniatures = generateurMiniatures;
        this.typePaiementId = typePaiementId;
        this.compteBancaireId = compteBancaireId;
    }

    // ─────────────────────────────── Employés ───────────────────────────────

    public ImportResultResponse importerEmployes(List<EmployeImportDto> employes) {
        List<LigneResultatImport> resultats = new ArrayList<>();

        if (employes == null || employes.isEmpty()) {
            return new ImportResultResponse(true, "Aucun employé à importer", 0, 0, resultats);
        }

        for (EmployeImportDto employe : employes) {
            resultats.add(importerUnEmploye(employe));
        }

        return synthetiser("Import des employés terminé", resultats);
    }

    private LigneResultatImport importerUnEmploye(EmployeImportDto employe) {
        String reference = employe.refEmploye();

        try {
            String erreurValidation = validerEmploye(employe);
            if (erreurValidation != null) {
                return LigneResultatImport.echec(reference, erreurValidation);
            }

            ResponseEntity<String> reponse = dolibarrClientService.appelerDolibarr(
                    "/users", HttpMethod.POST, construireCorpsEmploye(employe), String.class);

            Long idCree = extraireIdReponse(reponse.getBody());
            if (idCree == null) {
                return LigneResultatImport.echec(reference, "Réponse Dolibarr sans ID exploitable");
            }

            // Garde-fou : on ne touche jamais au superadmin.
            if (estIdProtege(idCree)) {
                return LigneResultatImport.echec(reference,
                        "ID Dolibarr protégé (" + idCree + "), employé ignoré");
            }

            importTrackingService.enregistrerEmploye(reference, idCree);
            return LigneResultatImport.succes(reference, idCree);
        } catch (Exception exception) {
            return LigneResultatImport.echec(reference, dolibarrClientService.gererErreurDolibarr(exception));
        }
    }

    private String validerEmploye(EmployeImportDto employe) {
        if (employe.refEmploye() == null || employe.refEmploye().isBlank()) {
            return "Référence employé manquante";
        }
        if (employe.identifiant() == null || employe.identifiant().isBlank()) {
            return "Identifiant (login) manquant";
        }
        if (employe.nom() == null || employe.nom().isBlank()) {
            return "Nom (lastname) manquant";
        }
        return null;
    }

    /**
     * Construit le corps de la requête POST /users.
     * Le mot de passe est transmis EN CLAIR : Dolibarr le hash à la réception. Le hacher
     * ici le rendrait inutilisable (double hachage). Il n'est jamais loggé ni tracké.
     * Aucun champ "id" n'est envoyé : Dolibarr attribue lui-même l'ID, le superadmin (1)
     * ne peut donc pas être écrasé.
     */
    private Map<String, Object> construireCorpsEmploye(EmployeImportDto employe) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("login", employe.identifiant());
        corps.put("password", employe.mdp());
        corps.put("lastname", employe.nom());
        if (employe.genre() != null && !employe.genre().isBlank()) {
            corps.put("gender", employe.genre()); // déjà converti homme->man / femme->woman côté front
        }
        if (employe.heureTravailSemaine() != null) {
            corps.put("weeklyhours", employe.heureTravailSemaine());
        }
        return corps;
    }

    // ─────────────────────────────── Salaires ───────────────────────────────

    public ImportResultResponse importerSalaires(List<SalaireImportDto> salaires) {
        List<LigneResultatImport> resultats = new ArrayList<>();

        if (salaires == null || salaires.isEmpty()) {
            return new ImportResultResponse(true, "Aucun salaire à importer", 0, 0, resultats);
        }

        for (SalaireImportDto salaire : salaires) {
            resultats.add(importerUnSalaire(salaire));
        }

        return synthetiser("Import des salaires terminé", resultats);
    }

    private LigneResultatImport importerUnSalaire(SalaireImportDto salaire) {
        String reference = salaire.refSalaire();

        try {
            if (reference == null || reference.isBlank()) {
                return LigneResultatImport.echec(reference, "Référence salaire manquante");
            }

            Long idEmploye = importTrackingService.resoudreEmploye(salaire.refEmploye());
            if (idEmploye == null) {
                return LigneResultatImport.echec(reference,
                        "Employé introuvable dans le tracking pour ref " + salaire.refEmploye()
                                + " (importer les employés d'abord)");
            }
            if (estIdProtege(idEmploye)) {
                return LigneResultatImport.echec(reference,
                        "Employé résolu vers un ID protégé (" + idEmploye + "), salaire ignoré");
            }

            ResponseEntity<String> reponse = dolibarrClientService.appelerDolibarr(
                    "/salaries", HttpMethod.POST, construireCorpsSalaire(salaire, idEmploye), String.class);

            Long idSalaire = extraireIdReponse(reponse.getBody());
            if (idSalaire == null) {
                return LigneResultatImport.echec(reference, "Réponse Dolibarr sans ID de salaire exploitable");
            }

            importTrackingService.enregistrerSalaire(reference, idSalaire);

            String messagePaiements = enregistrerPaiements(idSalaire, salaire.paiements());
            return new LigneResultatImport(reference, true, idSalaire, messagePaiements);
        } catch (Exception exception) {
            return LigneResultatImport.echec(reference, dolibarrClientService.gererErreurDolibarr(exception));
        }
    }

    /**
     * Construit le corps de la requête POST /salaries.
     * NOMS DE CHAMPS À CONFIRMER sur le Swagger de l'instance (peuvent varier selon version).
     */
    private Map<String, Object> construireCorpsSalaire(SalaireImportDto salaire, Long idEmploye) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("fk_user", idEmploye);
        corps.put("label", "Salaire " + salaire.refSalaire());
        corps.put("amount", salaire.montant());
        corps.put("datesp", convertirDateTimestamp(salaire.dateDebut())); // début de période
        corps.put("dateep", convertirDateTimestamp(salaire.dateFin()));   // fin de période
        return corps;
    }

    /**
     * Crée les paiements partiels d'un salaire. Chaque paiement en échec est compté
     * mais ne bloque pas les autres ; un résumé est renvoyé pour la ligne du salaire.
     */
    private String enregistrerPaiements(Long idSalaire, List<PaiementImportDto> paiements) {
        if (paiements == null || paiements.isEmpty()) {
            return "Aucun paiement (salaire non encore payé)";
        }

        int reussis = 0;
        List<String> echecs = new ArrayList<>();

        for (PaiementImportDto paiement : paiements) {
            try {
                dolibarrClientService.appelerDolibarr(
                        "/salaries/" + idSalaire + "/payments",
                        HttpMethod.POST,
                        construireCorpsPaiement(idSalaire, paiement),
                        String.class);
                reussis++;
            } catch (Exception exception) {
                echecs.add(dolibarrClientService.gererErreurDolibarr(exception));
            }
        }

        if (echecs.isEmpty()) {
            return reussis + " paiement(s) enregistré(s)";
        }
        return reussis + " paiement(s) enregistré(s), " + echecs.size() + " en échec : "
                + String.join(" | ", echecs);
    }

    /**
     * Construit le corps de la requête POST /salaries/{id}/payments.
     * Contrat vérifié sur la source Dolibarr (api_salaries / paymentsalary) :
     *  - champs obligatoires validés : paiementtype, datepaye, chid, amounts (+ accountid si module banque) ;
     *  - le montant se passe via une map "amounts" indexée par l'ID du salaire : { "<idSalaire>": montant } ;
     *  - le type de règlement doit être un ID numérique (c_paiement.id), passé en paiementtype ET fk_typepayment
     *    (la création persiste fk_typepayment) ;
     *  - chid reçoit l'ID du salaire.
     * accountid n'est ajouté que s'il est renseigné (> 0), pour les instances sans module banque.
     */
    private Map<String, Object> construireCorpsPaiement(Long idSalaire, PaiementImportDto paiement) {
        Map<String, Object> amounts = new LinkedHashMap<>();
        amounts.put(String.valueOf(idSalaire), paiement.montant());

        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("chid", idSalaire);
        corps.put("datepaye", convertirDateTimestamp(paiement.date()));
        corps.put("paiementtype", typePaiementId);
        corps.put("fk_typepayment", typePaiementId);
        corps.put("amounts", amounts);
        if (compteBancaireId != null && compteBancaireId > 0) {
            corps.put("accountid", compteBancaireId);
        }
        return corps;
    }

    // ─────────────────────────────── Photos ───────────────────────────────

    public ImportResultResponse importerPhotos(List<PhotoImportDto> photos) {
        List<LigneResultatImport> resultats = new ArrayList<>();

        if (photos == null || photos.isEmpty()) {
            return new ImportResultResponse(true, "Aucune photo à importer", 0, 0, resultats);
        }

        for (PhotoImportDto photo : photos) {
            resultats.add(importerUnePhoto(photo));
        }

        return synthetiser("Import des photos terminé", resultats);
    }

    /**
     * Workflow d'upload d'une photo (vérifié sur la source api_documents) :
     *  1. résolution ref_employe -> idUser via le tracking (sinon photo orpheline) ;
     *  2. upload de l'image originale renommée "photo.<ext>" dans {idUser}/photos ;
     *  3. mise à jour du champ photo du user ;
     *  4. génération des miniatures photo_small (270x480) et photo_mini (72x128) ;
     *  5. upload des miniatures dans {idUser}/photos/thumbs ;
     *  puis vérification d'accessibilité via GET /documents/download.
     * Garde-fou : jamais de photo pour le superadmin (ID 1) ni un ID protégé.
     */
    private LigneResultatImport importerUnePhoto(PhotoImportDto photo) {
        String reference = photo.refEmploye();

        try {
            if (reference == null || reference.isBlank()) {
                return LigneResultatImport.echec(reference, "Référence employé manquante");
            }

            Long idUser = importTrackingService.resoudreEmploye(reference);
            if (idUser == null) {
                return LigneResultatImport.echec(reference,
                        "Photo orpheline : aucun employé importé pour ref " + reference
                                + " (importer les employés d'abord)");
            }
            if (estIdProtege(idUser)) {
                return LigneResultatImport.echec(reference,
                        "ID protégé (" + idUser + "), photo ignorée");
            }

            byte[] image = Base64.getDecoder().decode(nettoyerBase64(photo.contenuBase64()));
            String extension = extensionPhoto(photo.nomFichier());
            String nomCible = "photo." + extension; // convention Dolibarr : toujours "photo.*"

            // Étape 2 : upload de l'image originale.
            televerserDocument(nomCible, idUser + "/photos", image);

            // Étape 3 : mise à jour du champ photo du user.
            dolibarrClientService.appelerDolibarr(
                    "/users/" + idUser, HttpMethod.PUT, Map.of("photo", nomCible), Map.class);

            // Étapes 4 et 5 : miniatures (dimensions Dolibarr en hauteur x largeur).
            byte[] small = generateurMiniatures.redimensionner(image, 270, 480, extension);
            byte[] mini = generateurMiniatures.redimensionner(image, 72, 128, extension);
            televerserDocument("photo_small." + extension, idUser + "/photos/thumbs", small);
            televerserDocument("photo_mini." + extension, idUser + "/photos/thumbs", mini);

            // Vérification d'accessibilité de la photo.
            boolean accessible = dolibarrClientService.fichierAccessible("user", idUser + "/photos/" + nomCible);
            return new LigneResultatImport(reference, true, idUser,
                    accessible ? "Photo OK (accessible)" : "Photo uploadée mais vérification KO");
        } catch (IllegalArgumentException exception) {
            return LigneResultatImport.echec(reference, "Contenu base64 invalide : " + exception.getMessage());
        } catch (Exception exception) {
            return LigneResultatImport.echec(reference, dolibarrClientService.gererErreurDolibarr(exception));
        }
    }

    /** Téléverse un fichier via POST /documents/upload (modulepart user, contenu base64). */
    private void televerserDocument(String filename, String subdir, byte[] contenu) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("filename", filename);
        corps.put("modulepart", "user");
        corps.put("subdir", subdir);
        corps.put("filecontent", Base64.getEncoder().encodeToString(contenu));
        corps.put("fileencoding", "base64");
        corps.put("overwriteifexists", 1);
        dolibarrClientService.appelerDolibarr("/documents/upload", HttpMethod.POST, corps, String.class);
    }

    /** Déduit l'extension cible (png par défaut, jpg si l'original est un JPEG). */
    private String extensionPhoto(String nomFichier) {
        String nom = nomFichier == null ? "" : nomFichier.toLowerCase(Locale.ROOT);
        if (nom.endsWith(".jpg") || nom.endsWith(".jpeg")) {
            return "jpg";
        }
        return "png";
    }

    /** Retire un éventuel préfixe "data:image/...;base64," avant le décodage. */
    private String nettoyerBase64(String contenu) {
        if (contenu == null) {
            return "";
        }
        if (contenu.startsWith("data:")) {
            int virgule = contenu.indexOf(',');
            if (virgule >= 0) {
                return contenu.substring(virgule + 1);
            }
        }
        return contenu;
    }

    // ─────────────────────────────── Outils ───────────────────────────────

    /** Vrai si l'ID correspond au superadmin ou à un utilisateur protégé par configuration. */
    private boolean estIdProtege(Long id) {
        if (id == null) {
            return false;
        }
        return id == ID_SUPERADMIN || dolibarrProperties.getProtectedUserIds().contains(id);
    }

    /**
     * Convertit une date du CSV en timestamp Unix (secondes) attendu par Dolibarr :
     * l'API ne convertit pas les chaînes, et create() appelle idate() qui exige un timestamp.
     * Gère jj/mm/aa, jj/mm/aaaa et le format ISO yyyy-MM-dd. La date est fixée à minuit UTC.
     * Renvoie null si le format n'est pas reconnu.
     */
    private Long convertirDateTimestamp(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }

        String valeur = date.trim();
        for (DateTimeFormatter format : List.of(
                DateTimeFormatter.ofPattern("dd/MM/yy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"))) {
            try {
                return LocalDate.parse(valeur, format).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            } catch (Exception ignore) {
                // on essaie le format suivant
            }
        }

        logger.warn("Format de date non reconnu, ignoré : {}", valeur);
        return null;
    }

    /**
     * Extrait l'ID renvoyé par Dolibarr après un POST. Selon l'endpoint, la réponse est
     * soit l'entier brut (ex : "42"), soit un objet JSON contenant id/rowid.
     */
    private Long extraireIdReponse(String corps) {
        if (corps == null || corps.isBlank()) {
            return null;
        }

        String texte = corps.trim().replace("\"", "");
        try {
            return Long.parseLong(texte);
        } catch (NumberFormatException ignore) {
            // la réponse n'est pas un entier brut : on tente d'y lire id/rowid
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> objet = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(corps, Map.class);
            return dolibarrClientService.extraireId(objet);
        } catch (Exception exception) {
            logger.error("Impossible d'extraire l'ID de la réponse Dolibarr : {}", corps);
            return null;
        }
    }

    private ImportResultResponse synthetiser(String libelle, List<LigneResultatImport> resultats) {
        int importees = (int) resultats.stream().filter(LigneResultatImport::succes).count();
        int erreurs = resultats.size() - importees;
        boolean success = erreurs == 0;
        String message = libelle + " : " + importees + " importée(s), " + erreurs + " en erreur";
        return new ImportResultResponse(success, message, importees, erreurs, resultats);
    }
}
