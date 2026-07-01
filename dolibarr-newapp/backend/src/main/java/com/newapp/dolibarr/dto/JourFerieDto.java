package com.newapp.dolibarr.dto;

import java.time.LocalDate;

public record JourFerieDto(
        Long id,
        String nom,
        LocalDate dateJour,
        String description,
        Boolean actif
) {
}
