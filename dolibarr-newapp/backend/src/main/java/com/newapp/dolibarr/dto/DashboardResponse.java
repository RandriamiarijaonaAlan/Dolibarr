package com.newapp.dolibarr.dto;

import java.util.List;

public record DashboardResponse(
        List<MontantParGenreResponse> montantParGenre,
        List<MontantParMoisResponse> montantParMois
) {
}
