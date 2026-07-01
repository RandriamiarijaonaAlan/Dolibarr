package com.newapp.dolibarr.controller;

import com.newapp.dolibarr.dto.CodeBackofficeRequest;
import com.newapp.dolibarr.dto.CodeBackofficeResponse;
import com.newapp.dolibarr.dto.DashboardResponse;
import com.newapp.dolibarr.dto.ReinitialisationResponse;
import com.newapp.dolibarr.service.BackofficeService;
import com.newapp.dolibarr.service.DashboardService;
import com.newapp.dolibarr.service.DolibarrResetService;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backoffice")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class BackofficeController {

    private final BackofficeService backofficeService;
    private final DolibarrResetService dolibarrResetService;
    private final DashboardService dashboardService;

    public BackofficeController(
            BackofficeService backofficeService,
            DolibarrResetService dolibarrResetService,
            DashboardService dashboardService
    ) {
        this.backofficeService = backofficeService;
        this.dolibarrResetService = dolibarrResetService;
        this.dashboardService = dashboardService;
    }

    @PostMapping("/check-code")
    public CodeBackofficeResponse verifierCode(@RequestBody CodeBackofficeRequest request) {
        boolean autorise = backofficeService.verifierCode(request.code());

        if (autorise) {
            return new CodeBackofficeResponse(true, "Code backoffice valide");
        }

        return new CodeBackofficeResponse(false, "Code backoffice invalide");
    }

    @PostMapping("/reset-data")
    public ReinitialisationResponse reinitialiserDonnees(
            @RequestHeader(value = "X-BACKOFFICE-CODE", required = false) String code
    ) {
        if (!backofficeService.verifierCode(code)) {
            return new ReinitialisationResponse(
                    false,
                    "Code backoffice invalide",
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    List.of("Reinitialisation refusee")
            );
        }

        return dolibarrResetService.reinitialiserDonnees();
    }

    @GetMapping("/dashboard")
    public DashboardResponse recupererDashboard() {
        return dashboardService.recupererDonneesDashboard();
    }
}
