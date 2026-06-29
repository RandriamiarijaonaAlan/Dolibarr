package com.newapp.dolibarr.dto;

import java.util.List;

/** Corps de la requête POST /api/import/employes : la liste des lignes valides. */
public record ImportEmployesRequest(
        List<EmployeImportDto> employes
) {
}
