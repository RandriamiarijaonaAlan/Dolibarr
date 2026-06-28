package com.newapp.dolibarr.dto;

import java.math.BigDecimal;

public record MontantParGenreResponse(
        String genre,
        BigDecimal montantTotal
) {
}
