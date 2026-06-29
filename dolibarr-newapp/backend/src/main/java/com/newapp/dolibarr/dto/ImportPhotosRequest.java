package com.newapp.dolibarr.dto;

import java.util.List;

/** Corps de la requête POST /api/import/photos : la liste des photos extraites du zip. */
public record ImportPhotosRequest(
        List<PhotoImportDto> photos
) {
}
