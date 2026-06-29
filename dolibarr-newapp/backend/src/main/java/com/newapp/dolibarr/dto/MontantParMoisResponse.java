package com.newapp.dolibarr.dto;

import java.math.BigDecimal;

public record MontantParMoisResponse(
        String mois,
        BigDecimal montantTotal
) {
}
