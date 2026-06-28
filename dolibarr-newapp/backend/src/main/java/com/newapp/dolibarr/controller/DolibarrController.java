package com.newapp.dolibarr.controller;

import com.newapp.dolibarr.dto.DolibarrConnectionResponse;
import com.newapp.dolibarr.service.DolibarrClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dolibarr")
public class DolibarrController {

    private final DolibarrClientService dolibarrClientService;

    public DolibarrController(DolibarrClientService dolibarrClientService) {
        this.dolibarrClientService = dolibarrClientService;
    }

    @GetMapping("/test-connexion")
    public DolibarrConnectionResponse testConnexion() {
        return dolibarrClientService.testConnection();
    }
}
