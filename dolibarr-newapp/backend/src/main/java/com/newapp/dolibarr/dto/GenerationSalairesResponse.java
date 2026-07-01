package com.newapp.dolibarr.dto;

public record GenerationSalairesResponse(
        boolean success,
        String message,
        ResumeGenerationSalairesDto resume
) {
}
