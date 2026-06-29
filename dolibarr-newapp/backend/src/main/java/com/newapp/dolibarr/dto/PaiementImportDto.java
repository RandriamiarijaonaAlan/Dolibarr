package com.newapp.dolibarr.dto;

/**
 * Un paiement partiel d'un salaire, issu du parsing du champ "paiement" du CSV.
 * La date est au format reçu du frontend (jj/mm/aa), le montant est un décimal.
 */
public record PaiementImportDto(
        String date,
        Double montant
) {
}
