package com.newapp.dolibarr.controller;

import com.newapp.dolibarr.dto.JourFerieDto;
import com.newapp.dolibarr.service.BackofficeService;
import com.newapp.dolibarr.service.JourFerieService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backoffice/jours-feries")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class JourFerieController {

    private final JourFerieService jourFerieService;
    private final BackofficeService backofficeService;

    public JourFerieController(JourFerieService jourFerieService, BackofficeService backofficeService) {
        this.jourFerieService = jourFerieService;
        this.backofficeService = backofficeService;
    }

    @GetMapping
    public List<JourFerieDto> lister() {
        return jourFerieService.lister();
    }

    @GetMapping("/{id}")
    public JourFerieDto trouver(@PathVariable Long id) {
        return jourFerieService.trouver(id);
    }

    @PostMapping
    public ResponseEntity<?> creer(
            @RequestHeader(value = "X-BACKOFFICE-CODE", required = false) String code,
            @RequestBody JourFerieDto dto
    ) {
        if (!backofficeService.verifierCode(code)) {
            return erreur(HttpStatus.UNAUTHORIZED, "Code backoffice invalide");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(jourFerieService.creer(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(
            @RequestHeader(value = "X-BACKOFFICE-CODE", required = false) String code,
            @PathVariable Long id,
            @RequestBody JourFerieDto dto
    ) {
        if (!backofficeService.verifierCode(code)) {
            return erreur(HttpStatus.UNAUTHORIZED, "Code backoffice invalide");
        }

        return ResponseEntity.ok(jourFerieService.modifier(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(
            @RequestHeader(value = "X-BACKOFFICE-CODE", required = false) String code,
            @PathVariable Long id
    ) {
        if (!backofficeService.verifierCode(code)) {
            return erreur(HttpStatus.UNAUTHORIZED, "Code backoffice invalide");
        }

        jourFerieService.supprimer(id);
        return ResponseEntity.ok(Map.of("message", "Jour férié supprimé"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> gererErreur(IllegalArgumentException exception) {
        return erreur(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> erreur(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(Map.of("message", message));
    }
}
