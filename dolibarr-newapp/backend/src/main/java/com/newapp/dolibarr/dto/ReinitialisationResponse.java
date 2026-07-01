package com.newapp.dolibarr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReinitialisationResponse(
        boolean success,
        String message,
        int usersDeleted,
        int usersSkippedProtected,
        int usersSkippedNotNewApp,
        int salariesDeleted,
        int paymentsDeleted,
        int joursFeriesDeleted,
        List<String> errors
) {
}
