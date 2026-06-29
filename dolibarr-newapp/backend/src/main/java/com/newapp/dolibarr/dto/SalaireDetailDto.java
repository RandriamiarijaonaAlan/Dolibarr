package com.newapp.dolibarr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

/**
 * Détail d'un salaire pour la fiche de paiement : infos employé, montant, période,
 * historique des paiements, reste à payer et statut (solde / partiel / impayé).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalaireDetailDto(
        Long id,
        Long idEmploye,
        String nomEmploye,
        String loginEmploye,
        String genreEmploye,
        BigDecimal montant,
        String dateDebut,
        String dateFin,
        BigDecimal totalPaye,
        BigDecimal resteAPayer,
        String statut,
        List<PaiementDetailDto> paiements
) {
}
