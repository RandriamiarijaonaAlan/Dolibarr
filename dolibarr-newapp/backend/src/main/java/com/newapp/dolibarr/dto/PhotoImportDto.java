package com.newapp.dolibarr.dto;

/**
 * Une photo issue du zip, identifiée par le ref_employe déduit du nom de fichier
 * (ex : "1.png" -> refEmploye "1"). Le contenu est transmis en base64 par le frontend.
 */
public record PhotoImportDto(
        String refEmploye,
        String nomFichier,
        String contenuBase64
) {
}
