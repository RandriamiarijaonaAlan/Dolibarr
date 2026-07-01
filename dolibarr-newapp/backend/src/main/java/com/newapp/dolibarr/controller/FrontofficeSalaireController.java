package com.newapp.dolibarr.controller;

import com.newapp.dolibarr.dto.GenerationSalairesResponse;
import com.newapp.dolibarr.dto.GenererSalairesRequest;
import com.newapp.dolibarr.service.SalaireGenerationService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/frontoffice")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class FrontofficeSalaireController {

    private final SalaireGenerationService salaireGenerationService;

    public FrontofficeSalaireController(SalaireGenerationService salaireGenerationService) {
        this.salaireGenerationService = salaireGenerationService;
    }

    @PostMapping("/salaires/generer")
    public GenerationSalairesResponse genererSalaires(@RequestBody GenererSalairesRequest request) {
        return salaireGenerationService.genererSalaires(request);
    }
}
