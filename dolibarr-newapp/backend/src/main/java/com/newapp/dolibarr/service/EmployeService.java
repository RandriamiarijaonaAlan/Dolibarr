package com.newapp.dolibarr.service;

import com.newapp.dolibarr.dto.EmployeListeDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EmployeService {

    private static final long ID_SUPERADMIN = 1L;

    private final DolibarrClientService dolibarrClientService;

    public EmployeService(DolibarrClientService dolibarrClientService) {
        this.dolibarrClientService = dolibarrClientService;
    }

    public List<EmployeListeDto> rechercherEmployes(String poste, String genre, Double heureMin, Double heureMax) {
        List<EmployeListeDto> employes = new ArrayList<>();
        for (Object ressource : dolibarrClientService.listerRessources("/users")) {
            if (!(ressource instanceof Map<?, ?> donnees)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) donnees;
            Long id = dolibarrClientService.extraireId(user);
            if (id == null || id == ID_SUPERADMIN) {
                continue;
            }

            EmployeListeDto employe = new EmployeListeDto(
                    id,
                    texte(valeur(user, "ref", "ref_employee", "employee_ref", "rowid", "id")),
                    texte(valeur(user, "lastname", "nom")),
                    texte(valeur(user, "login")),
                    texte(valeur(user, "job", "poste")),
                    texte(valeur(user, "gender", "genre")),
                    convertirDouble(valeur(user, "weeklyhours")),
                    estActif(user),
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );

            if (!filtrerParPoste(employe, poste)) {
                continue;
            }
            if (!filtrerParGenre(employe, genre)) {
                continue;
            }
            if (!filtrerParHeures(employe, heureMin, heureMax)) {
                continue;
            }
            employes.add(employe);
        }

        employes.sort(Comparator.comparing(employe -> texte(employe.nom()).toLowerCase()));
        return employes;
    }

    public boolean filtrerParPoste(EmployeListeDto employe, String poste) {
        return poste == null || poste.isBlank()
                || texte(employe.poste()).toLowerCase().contains(poste.trim().toLowerCase());
    }

    public boolean filtrerParGenre(EmployeListeDto employe, String genre) {
        if (genre == null || genre.isBlank() || "tous".equalsIgnoreCase(genre)) {
            return true;
        }
        String valeur = genre.trim().toLowerCase();
        if ("homme".equals(valeur)) {
            valeur = "man";
        } else if ("femme".equals(valeur)) {
            valeur = "woman";
        }
        return valeur.equalsIgnoreCase(texte(employe.genre()));
    }

    public boolean filtrerParHeures(EmployeListeDto employe, Double heureMin, Double heureMax) {
        double heures = employe.heuresSemaine() == null ? 0 : employe.heuresSemaine();
        if (heureMin != null && heures < heureMin) {
            return false;
        }
        return heureMax == null || heures <= heureMax;
    }

    private boolean estActif(Map<String, Object> user) {
        Object statut = valeur(user, "statut", "status", "active");
        if (statut == null) {
            return true;
        }
        String texte = statut.toString().trim();
        return "1".equals(texte) || "true".equalsIgnoreCase(texte) || "active".equalsIgnoreCase(texte);
    }

    private Object valeur(Map<String, Object> objet, String... cles) {
        for (String cle : cles) {
            Object valeur = objet.get(cle);
            if (valeur != null && !valeur.toString().isBlank()) {
                return valeur;
            }
        }
        return null;
    }

    private Double convertirDouble(Object valeur) {
        if (valeur == null) {
            return null;
        }
        if (valeur instanceof Number nombre) {
            return nombre.doubleValue();
        }
        try {
            return Double.parseDouble(valeur.toString().trim().replace(",", "."));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String texte(Object valeur) {
        return valeur == null ? "" : valeur.toString();
    }
}
