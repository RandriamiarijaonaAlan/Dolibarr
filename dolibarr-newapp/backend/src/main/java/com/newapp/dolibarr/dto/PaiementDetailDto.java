package com.newapp.dolibarr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** Un paiement déjà effectué sur un salaire, pour l'historique (date ISO, montant, mode lisible). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaiementDetailDto(
        String date,
        BigDecimal montant,
        String mode
) {
}
