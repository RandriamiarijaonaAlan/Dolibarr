package com.newapp.dolibarr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Rapport global d'un import (employés ou salaires) renvoyé au frontend :
 * nombre de lignes importées, nombre d'erreurs et détail ligne par ligne.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImportResultResponse(
        boolean success,
        String message,
        int lignesImportees,
        int lignesEnErreur,
        List<LigneResultatImport> resultats
) {

    public static ImportResultResponse refuse(String message) {
        return new ImportResultResponse(false, message, 0, 0, List.of());
    }
}
