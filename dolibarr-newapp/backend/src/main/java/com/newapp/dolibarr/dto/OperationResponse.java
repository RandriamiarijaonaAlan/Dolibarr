package com.newapp.dolibarr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Réponse générique d'une opération d'écriture (création salaire/paiement) du front-office. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OperationResponse(
        boolean success,
        String message,
        Long id
) {

    public static OperationResponse ok(String message, Long id) {
        return new OperationResponse(true, message, id);
    }

    public static OperationResponse echec(String message) {
        return new OperationResponse(false, message, null);
    }
}
