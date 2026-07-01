package com.newapp.dolibarr.dto;

import java.util.List;

public record ResumeGenerationSalairesDto(
        int salairesCrees,
        int salairesIgnores,
        List<String> erreurs
) {
}
