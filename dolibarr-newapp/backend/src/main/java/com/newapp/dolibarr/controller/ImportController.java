package com.newapp.dolibarr.controller;

import com.newapp.dolibarr.dto.ImportEmployesRequest;
import com.newapp.dolibarr.dto.ImportResultResponse;
import com.newapp.dolibarr.dto.ImportSalairesRequest;
import com.newapp.dolibarr.service.BackofficeService;
import com.newapp.dolibarr.service.DolibarrImportService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Routes d'import du backoffice. Comme la réinitialisation, ces routes sont protégées
 * par le code backoffice transmis dans l'en-tête X-BACKOFFICE-CODE (même mécanisme que
 * {@link BackofficeController#reinitialiserDonnees}). La clé API Dolibarr reste côté
 * backend, jamais exposée au frontend.
 */
@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class ImportController {

    private final BackofficeService backofficeService;
    private final DolibarrImportService dolibarrImportService;

    public ImportController(
            BackofficeService backofficeService,
            DolibarrImportService dolibarrImportService
    ) {
        this.backofficeService = backofficeService;
        this.dolibarrImportService = dolibarrImportService;
    }

    @PostMapping("/employes")
    public ImportResultResponse importerEmployes(
            @RequestHeader(value = "X-BACKOFFICE-CODE", required = false) String code,
            @RequestBody ImportEmployesRequest request
    ) {
        if (!backofficeService.verifierCode(code)) {
            return ImportResultResponse.refuse("Code backoffice invalide");
        }

        return dolibarrImportService.importerEmployes(request.employes());
    }

    @PostMapping("/salaires")
    public ImportResultResponse importerSalaires(
            @RequestHeader(value = "X-BACKOFFICE-CODE", required = false) String code,
            @RequestBody ImportSalairesRequest request
    ) {
        if (!backofficeService.verifierCode(code)) {
            return ImportResultResponse.refuse("Code backoffice invalide");
        }

        return dolibarrImportService.importerSalaires(request.salaires());
    }
}
