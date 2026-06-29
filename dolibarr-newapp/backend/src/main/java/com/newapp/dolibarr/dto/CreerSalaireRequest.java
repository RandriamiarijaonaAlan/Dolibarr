package com.newapp.dolibarr.dto;

/** Corps de POST /api/frontoffice/salaries : création d'un salaire. Dates au format ISO YYYY-MM-DD. */
public record CreerSalaireRequest(
        Long fkUser,
        Double montant,
        String dateDebut,
        String dateFin
) {
}
