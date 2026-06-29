package com.newapp.dolibarr.controller;

import com.newapp.dolibarr.dto.CreerPaiementRequest;
import com.newapp.dolibarr.dto.CreerSalaireRequest;
import com.newapp.dolibarr.dto.EmployeListeDto;
import com.newapp.dolibarr.dto.ModePaiementDto;
import com.newapp.dolibarr.dto.OperationResponse;
import com.newapp.dolibarr.dto.SalaireDetailDto;
import com.newapp.dolibarr.service.FrontOfficeService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Routes du front-office (consultation et gestion des salaires). La clé API Dolibarr
 * reste côté backend : le frontend ne consomme que ces routes proxy.
 */
@RestController
@RequestMapping("/api/frontoffice")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class FrontOfficeController {

    private final FrontOfficeService frontOfficeService;

    public FrontOfficeController(FrontOfficeService frontOfficeService) {
        this.frontOfficeService = frontOfficeService;
    }

    @GetMapping("/employes")
    public List<EmployeListeDto> listerEmployes() {
        return frontOfficeService.listerEmployes();
    }

    @GetMapping("/employes/{id}/salaires")
    public List<SalaireDetailDto> listerSalairesEmploye(@PathVariable Long id) {
        return frontOfficeService.listerSalairesEmploye(id);
    }

    @GetMapping("/salaires/{id}")
    public SalaireDetailDto detailSalaire(@PathVariable Long id) {
        return frontOfficeService.detailSalaire(id);
    }

    @PostMapping("/salaires")
    public OperationResponse creerSalaire(@RequestBody CreerSalaireRequest requete) {
        return frontOfficeService.creerSalaire(requete);
    }

    @PostMapping("/salaires/{id}/paiements")
    public OperationResponse ajouterPaiement(
            @PathVariable Long id,
            @RequestBody CreerPaiementRequest requete
    ) {
        return frontOfficeService.ajouterPaiement(id, requete);
    }

    @GetMapping("/modes-paiement")
    public List<ModePaiementDto> listerModesPaiement() {
        return frontOfficeService.listerModesPaiement();
    }

    @GetMapping("/employes/{id}/photo")
    public ResponseEntity<byte[]> photoEmploye(@PathVariable Long id) {
        FrontOfficeService.FichierBinaire photo = frontOfficeService.photoEmploye(id);
        if (photo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.typeMime()))
                .body(photo.contenu());
    }
}
