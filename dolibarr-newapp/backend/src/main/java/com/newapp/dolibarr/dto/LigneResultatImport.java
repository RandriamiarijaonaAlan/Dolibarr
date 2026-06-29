package com.newapp.dolibarr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Résultat de l'import d'une seule ligne. Permet de remonter au frontend le détail
 * ligne par ligne : succès avec l'ID Dolibarr créé, ou échec avec le message d'erreur.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LigneResultatImport(
        String reference,
        boolean succes,
        Long idDolibarr,
        String message
) {

    public static LigneResultatImport succes(String reference, Long idDolibarr) {
        return new LigneResultatImport(reference, true, idDolibarr, null);
    }

    public static LigneResultatImport echec(String reference, String message) {
        return new LigneResultatImport(reference, false, null, message);
    }
}
