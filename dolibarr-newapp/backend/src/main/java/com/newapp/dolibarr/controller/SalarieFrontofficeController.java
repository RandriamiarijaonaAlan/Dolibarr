package com.newapp.dolibarr.controller;

import com.newapp.dolibarr.dto.DetailSalarieFrontofficeDto;
import com.newapp.dolibarr.dto.SalarieFrontofficeDto;
import com.newapp.dolibarr.service.SalarieFrontofficeService;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/frontoffice/salaries")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class SalarieFrontofficeController {

    private final SalarieFrontofficeService salarieFrontofficeService;

    public SalarieFrontofficeController(SalarieFrontofficeService salarieFrontofficeService) {
        this.salarieFrontofficeService = salarieFrontofficeService;
    }

    @GetMapping
    public List<SalarieFrontofficeDto> recupererTousLesSalaries() {
        return salarieFrontofficeService.recupererTousLesSalaries();
    }

    @GetMapping("/{id}")
    public DetailSalarieFrontofficeDto recupererDetailSalarie(@PathVariable Long id) {
        return salarieFrontofficeService.recupererDetailSalarie(id);
    }
}
