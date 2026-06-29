package com.newapp.dolibarr.dto;

import java.util.List;

/**
 * Une ligne du fichier CSV des salaires, déjà parsée et validée côté frontend.
 * refEmploye référence un employé du premier fichier ; il est résolu vers l'ID
 * Dolibarr réel via le fichier de tracking au moment de l'import.
 */
public record SalaireImportDto(
        String refSalaire,
        String refEmploye,
        String dateDebut,
        String dateFin,
        Double montant,
        List<PaiementImportDto> paiements
) {
}
