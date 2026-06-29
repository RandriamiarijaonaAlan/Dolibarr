package com.newapp.dolibarr.dto;

import java.util.List;

/** Corps de la requête POST /api/import/salaires : la liste des lignes valides. */
public record ImportSalairesRequest(
        List<SalaireImportDto> salaires
) {
}
