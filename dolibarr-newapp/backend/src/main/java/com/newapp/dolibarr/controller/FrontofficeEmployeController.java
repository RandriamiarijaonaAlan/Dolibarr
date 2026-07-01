package com.newapp.dolibarr.controller;

import com.newapp.dolibarr.dto.EmployeListeDto;
import com.newapp.dolibarr.service.EmployeService;
import com.newapp.dolibarr.service.FrontOfficeService;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/frontoffice")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class FrontofficeEmployeController {

    private final EmployeService employeService;
    private final FrontOfficeService frontOfficeService;

    public FrontofficeEmployeController(EmployeService employeService, FrontOfficeService frontOfficeService) {
        this.employeService = employeService;
        this.frontOfficeService = frontOfficeService;
    }

    @GetMapping("/employes")
    public List<EmployeListeDto> rechercherEmployes(
            @RequestParam(required = false) String poste,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Double heureMin,
            @RequestParam(required = false) Double heureMax
    ) {
        if (poste == null && genre == null && heureMin == null && heureMax == null) {
            return frontOfficeService.listerEmployes();
        }

        return employeService.rechercherEmployes(poste, genre, heureMin, heureMax);
    }
}
