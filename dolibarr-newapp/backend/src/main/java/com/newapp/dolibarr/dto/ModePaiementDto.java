package com.newapp.dolibarr.dto;

/** Un mode de paiement Dolibarr (fk_typepayment) pour le dropdown du formulaire de paiement. */
public record ModePaiementDto(
        int id,
        String libelle
) {
}
