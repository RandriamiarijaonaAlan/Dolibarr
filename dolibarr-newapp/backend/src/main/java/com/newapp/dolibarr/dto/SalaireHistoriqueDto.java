package com.newapp.dolibarr.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalaireHistoriqueDto(
        Long idSalaire,
        String refSalaire,
        String dateDebut,
        String dateFin,
        BigDecimal montantSalaire,
        BigDecimal totalPaye,
        BigDecimal resteAPayer,
        String statutPaiement,
        List<PaiementHistoriqueDto> paiements
) {
}
