package com.newapp.dolibarr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DolibarrConnectionResponse(
        boolean connected,
        String message,
        String dolibarrUrl,
        String details
) {
    public static DolibarrConnectionResponse success(String dolibarrUrl) {
        return new DolibarrConnectionResponse(
                true,
                "Connexion Dolibarr réussie",
                dolibarrUrl,
                null
        );
    }

    public static DolibarrConnectionResponse error(String dolibarrUrl, String details) {
        return new DolibarrConnectionResponse(
                false,
                "Erreur de connexion à Dolibarr",
                null,
                details
        );
    }
}
