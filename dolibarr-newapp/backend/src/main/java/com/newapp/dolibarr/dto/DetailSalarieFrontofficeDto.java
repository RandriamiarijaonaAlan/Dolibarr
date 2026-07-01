package com.newapp.dolibarr.dto;

import java.util.List;

public record DetailSalarieFrontofficeDto(
        SalarieFrontofficeDto salarie,
        List<SalaireHistoriqueDto> historiquesSalaires
) {
}
