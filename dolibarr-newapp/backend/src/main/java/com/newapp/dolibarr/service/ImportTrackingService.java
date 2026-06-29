package com.newapp.dolibarr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Maintient le fichier JSON simple qui trace la correspondance entre les références
 * du CSV et les IDs réellement créés dans Dolibarr. Ce fichier est la seule donnée
 * persistée côté NewApp : Dolibarr reste la source de vérité. Il permet notamment
 * de résoudre ref_employe -> ID Dolibarr lors de l'import des salaires, et un reset
 * ciblé ultérieur.
 *
 * Structure du fichier :
 * {
 *   "employes": { "ref_employe": idDolibarr, ... },
 *   "salaires": { "ref_salaire": idDolibarr, ... }
 * }
 *
 * Aucune donnée sensible (mot de passe) n'est jamais écrite ici.
 */
@Service
public class ImportTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(ImportTrackingService.class);

    private static final String CLE_EMPLOYES = "employes";
    private static final String CLE_SALAIRES = "salaires";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path cheminFichier;

    public ImportTrackingService(@Value("${import.fichier-tracking}") String fichierTracking) {
        this.cheminFichier = Paths.get(fichierTracking);
    }

    /** Enregistre la correspondance ref_employe -> ID Dolibarr. */
    public synchronized void enregistrerEmploye(String refEmploye, Long idDolibarr) {
        enregistrer(CLE_EMPLOYES, refEmploye, idDolibarr);
    }

    /** Enregistre la correspondance ref_salaire -> ID Dolibarr. */
    public synchronized void enregistrerSalaire(String refSalaire, Long idDolibarr) {
        enregistrer(CLE_SALAIRES, refSalaire, idDolibarr);
    }

    /** Résout une référence employé du CSV vers son ID Dolibarr, ou null si inconnue. */
    public synchronized Long resoudreEmploye(String refEmploye) {
        Map<String, Map<String, Long>> donnees = lireDonnees();
        Map<String, Long> employes = donnees.get(CLE_EMPLOYES);
        return employes == null ? null : employes.get(refEmploye);
    }

    private void enregistrer(String section, String reference, Long idDolibarr) {
        if (reference == null || reference.isBlank() || idDolibarr == null) {
            return;
        }

        Map<String, Map<String, Long>> donnees = lireDonnees();
        donnees.computeIfAbsent(section, cle -> new LinkedHashMap<>()).put(reference, idDolibarr);
        ecrireDonnees(donnees);
    }

    private Map<String, Map<String, Long>> lireDonnees() {
        if (!Files.exists(cheminFichier)) {
            return nouvellesDonnees();
        }

        try {
            byte[] contenu = Files.readAllBytes(cheminFichier);
            if (contenu.length == 0) {
                return nouvellesDonnees();
            }

            return objectMapper.readValue(contenu, new TypeReference<LinkedHashMap<String, Map<String, Long>>>() {
            });
        } catch (IOException exception) {
            logger.error("Lecture du fichier de tracking impossible : {}", exception.getMessage());
            return nouvellesDonnees();
        }
    }

    private void ecrireDonnees(Map<String, Map<String, Long>> donnees) {
        try {
            if (cheminFichier.getParent() != null) {
                Files.createDirectories(cheminFichier.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(cheminFichier.toFile(), donnees);
        } catch (IOException exception) {
            logger.error("Écriture du fichier de tracking impossible : {}", exception.getMessage());
        }
    }

    private Map<String, Map<String, Long>> nouvellesDonnees() {
        Map<String, Map<String, Long>> donnees = new LinkedHashMap<>();
        donnees.put(CLE_EMPLOYES, new LinkedHashMap<>());
        donnees.put(CLE_SALAIRES, new LinkedHashMap<>());
        return donnees;
    }
}
