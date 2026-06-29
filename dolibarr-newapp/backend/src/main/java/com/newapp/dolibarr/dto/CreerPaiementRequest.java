package com.newapp.dolibarr.dto;

/** Corps de POST /api/frontoffice/salaries/{id}/paiements : un paiement partiel. Date ISO YYYY-MM-DD. */
public record CreerPaiementRequest(
        Double montant,
        String date,
        Integer fkTypePayment
) {
}
