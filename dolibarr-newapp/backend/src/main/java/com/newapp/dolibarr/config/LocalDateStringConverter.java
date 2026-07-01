package com.newapp.dolibarr.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

@Converter
public class LocalDateStringConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate valeur) {
        return valeur == null ? null : valeur.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String valeur) {
        return valeur == null || valeur.isBlank() ? null : LocalDate.parse(valeur);
    }
}
