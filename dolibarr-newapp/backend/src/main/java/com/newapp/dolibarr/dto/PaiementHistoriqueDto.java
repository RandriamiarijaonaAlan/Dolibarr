package com.newapp.dolibarr.dto;

import java.math.BigDecimal;

public record PaiementHistoriqueDto(
        Long idPaiement,
        String datePaiement,
        BigDecimal montantPaiement,
        String referencePaiement
) {
}
