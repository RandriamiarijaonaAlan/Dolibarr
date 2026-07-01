package com.newapp.dolibarr.dto;

/**
 * Une ligne du fichier CSV des employés, déjà parsée et validée côté frontend.
 * Le genre arrive déjà converti en valeur anglaise attendue par Dolibarr (man/woman).
 * Le mot de passe transite en clair : Dolibarr le hash lui-même à la réception.
 */
public record EmployeImportDto(
        String refEmploye,
        String nom,
        String genre,
        String identifiant,
        String mdp,
        Double heureTravailSemaine,
        String poste
) {
}
