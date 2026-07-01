package com.newapp.dolibarr.dto;

public record SalarieFrontofficeDto(
        Long id,
        String refEmploye,
        String nom,
        String prenom,
        String identifiant,
        String poste,
        String genre,
        Double heureTravailSemaine,
        String statut
) {
}
