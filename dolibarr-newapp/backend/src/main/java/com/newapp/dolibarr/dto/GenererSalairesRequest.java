package com.newapp.dolibarr.dto;

import java.util.List;

public record GenererSalairesRequest(
        List<Long> employeIds,
        String dateDebut,
        String dateFin,
        Double montant
) {
}
