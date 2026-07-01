package com.newapp.dolibarr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * Un salarié dans la liste du front-office, avec ses agrégats de salaires.
 * Le genre est renvoyé tel que stocké dans Dolibarr (man/woman) ; le frontend l'affiche.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmployeListeDto(
        Long id,
        String refEmploye,
        String nom,
        String login,
        String poste,
        String genre,
        Double heuresSemaine,
        boolean actif,
        int nbSalaires,
        BigDecimal montantTotal,
        BigDecimal resteAPayer
) {
}
