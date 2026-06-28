package com.newapp.dolibarr.service;

import com.newapp.dolibarr.dto.DashboardResponse;
import com.newapp.dolibarr.dto.MontantParGenreResponse;
import com.newapp.dolibarr.dto.MontantParMoisResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DolibarrClientService dolibarrClientService;

    public DashboardService(DolibarrClientService dolibarrClientService) {
        this.dolibarrClientService = dolibarrClientService;
    }

    public DashboardResponse recupererDonneesDashboard() {
        List<Map<String, Object>> paiements = convertirListe(dolibarrClientService.listerRessources("/salaries/payments"));
        Map<String, Map<String, Object>> salairesParId = indexerParId(convertirListe(dolibarrClientService.listerRessources("/salaries")));
        Map<String, Map<String, Object>> usersParId = indexerParId(convertirListe(dolibarrClientService.listerRessources("/users")));
        Set<String> paiementsComptesParGenre = new HashSet<>();
        Set<String> paiementsComptesParMois = new HashSet<>();

        Map<String, BigDecimal> montantsParGenre = calculerMontantParGenre(
                paiements,
                salairesParId,
                usersParId,
                paiementsComptesParGenre
        );
        Map<String, BigDecimal> montantsParMois = calculerMontantParMois(paiements, paiementsComptesParMois);

        return new DashboardResponse(
                convertirMontantsParGenre(montantsParGenre),
                convertirMontantsParMois(montantsParMois)
        );
    }

    public List<MontantParGenreResponse> recupererMontantSalaireParGenre() {
        return recupererDonneesDashboard().montantParGenre();
    }

    public List<MontantParMoisResponse> recupererMontantSalaireParMois() {
        return recupererDonneesDashboard().montantParMois();
    }

    public Map<String, BigDecimal> calculerMontantParGenre(
            List<Map<String, Object>> paiements,
            Map<String, Map<String, Object>> salairesParId,
            Map<String, Map<String, Object>> usersParId,
            Set<String> paiementsComptes
    ) {
        Map<String, BigDecimal> montants = new LinkedHashMap<>();

        for (Map<String, Object> paiement : paiements) {
            String clePaiement = clePaiement(paiement);
            if (paiementsComptes.contains(clePaiement)) {
                continue;
            }

            LocalDate datePaiement = convertirDate(getValeur(paiement,
                    "datep", "date_payment", "date_paid", "date_reglement", "date_r", "payment_date", "date"));
            if (datePaiement == null) {
                continue;
            }

            BigDecimal montant = convertirMontant(getValeur(paiement,
                    "amount", "amount_payment", "montant", "total", "total_ttc", "amount_ttc", "total_ht", "amount_ht"));
            if (montant == null) {
                continue;
            }

            Map<String, Object> salaire = trouverSalaire(paiement, salairesParId);
            if (salaire == null) {
                continue;
            }

            Map<String, Object> user = trouverUtilisateur(salaire, usersParId);
            if (user == null) {
                continue;
            }

            String genre = normaliserGenre(getValeur(user, "gender", "genre", "sexe", "civility_code", "civility"));
            paiementsComptes.add(clePaiement);
            additionnerMontant(montants, genre, montant);
        }

        return montants;
    }

    public Map<String, BigDecimal> calculerMontantParMois(List<Map<String, Object>> paiements, Set<String> paiementsComptes) {
        Map<String, BigDecimal> montants = new TreeMap<>();

        for (Map<String, Object> paiement : paiements) {
            String clePaiement = clePaiement(paiement);
            if (paiementsComptes.contains(clePaiement)) {
                continue;
            }

            LocalDate datePaiement = convertirDate(getValeur(paiement,
                    "datep", "date_payment", "date_paid", "date_reglement", "date_r", "payment_date", "date"));
            if (datePaiement == null) {
                continue;
            }

            BigDecimal montant = convertirMontant(getValeur(paiement,
                    "amount", "amount_payment", "montant", "total", "total_ttc", "amount_ttc", "total_ht", "amount_ht"));
            if (montant == null) {
                continue;
            }

            paiementsComptes.add(clePaiement);
            additionnerMontant(montants, formaterMois(datePaiement), montant);
        }

        return montants;
    }

    public Object getValeur(Map<String, Object> objet, String... cles) {
        for (String cle : cles) {
            Object valeur = objet.get(cle);
            if (valeur != null && !valeur.toString().isBlank()) {
                return valeur;
            }
        }

        return null;
    }

    public BigDecimal convertirMontant(Object valeur) {
        if (valeur == null) {
            return null;
        }

        if (valeur instanceof Number nombre) {
            return BigDecimal.valueOf(nombre.doubleValue());
        }

        String texte = valeur.toString().trim().replace(" ", "").replace(",", ".");
        if (texte.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(texte);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public LocalDate convertirDate(Object valeur) {
        if (valeur == null) {
            return null;
        }

        if (valeur instanceof Number nombre) {
            long timestamp = nombre.longValue();
            if (timestamp > 9_999_999_999L) {
                timestamp = timestamp / 1000;
            }
            return Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String texte = valeur.toString().trim();
        if (texte.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(texte.substring(0, 10));
        } catch (DateTimeParseException | IndexOutOfBoundsException exception) {
            return null;
        }
    }

    public void additionnerMontant(Map<String, BigDecimal> montants, String cle, BigDecimal montant) {
        montants.merge(cle, montant, BigDecimal::add);
    }

    public String formaterMois(LocalDate date) {
        return YearMonth.from(date).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private Map<String, Object> trouverSalaire(Map<String, Object> paiement, Map<String, Map<String, Object>> salairesParId) {
        Object idSalaire = getValeur(paiement, "fk_salary", "salary_id", "id_salary", "fk_salaryid", "fk_object", "fk_salarydet");
        if (idSalaire == null) {
            return null;
        }

        return salairesParId.get(idSalaire.toString());
    }

    private Map<String, Object> trouverUtilisateur(Map<String, Object> salaire, Map<String, Map<String, Object>> usersParId) {
        Object idUser = getValeur(salaire, "fk_user", "fk_userid", "user_id", "employee_id", "fk_employee", "fk_user_author");
        if (idUser == null) {
            return null;
        }

        return usersParId.get(idUser.toString());
    }

    private String normaliserGenre(Object valeur) {
        if (valeur == null) {
            return "non renseigné";
        }

        String genre = valeur.toString().trim().toLowerCase();
        if (genre.equals("man") || genre.equals("male") || genre.equals("m") || genre.equals("homme") || genre.equals("mr")) {
            return "homme";
        }

        if (genre.equals("woman") || genre.equals("female") || genre.equals("f") || genre.equals("femme") || genre.equals("mme") || genre.equals("mrs")) {
            return "femme";
        }

        return genre.isBlank() ? "non renseigné" : genre;
    }

    private String clePaiement(Map<String, Object> paiement) {
        Object id = getValeur(paiement, "id", "rowid", "pid", "payment_id");
        if (id != null) {
            return "id:" + id;
        }

        return "fallback:"
                + getValeur(paiement, "fk_salary", "salary_id", "fk_object")
                + "|"
                + getValeur(paiement, "datep", "date_payment", "date_paid", "date_reglement", "date_r", "payment_date", "date")
                + "|"
                + getValeur(paiement, "amount", "amount_payment", "montant", "total", "total_ttc", "amount_ttc");
    }

    private Map<String, Map<String, Object>> indexerParId(List<Map<String, Object>> ressources) {
        Map<String, Map<String, Object>> index = new HashMap<>();

        for (Map<String, Object> ressource : ressources) {
            Object id = getValeur(ressource, "id", "rowid");
            if (id != null) {
                index.put(id.toString(), ressource);
            }
        }

        return index;
    }

    private List<Map<String, Object>> convertirListe(List<?> ressources) {
        List<Map<String, Object>> resultat = new ArrayList<>();
        if (ressources == null) {
            return resultat;
        }

        for (Object ressource : ressources) {
            if (ressource instanceof Map<?, ?> map) {
                Map<String, Object> donnees = new HashMap<>();
                for (Map.Entry<?, ?> entree : map.entrySet()) {
                    if (entree.getKey() != null) {
                        donnees.put(entree.getKey().toString(), entree.getValue());
                    }
                }
                resultat.add(donnees);
            }
        }

        return resultat;
    }

    private List<MontantParGenreResponse> convertirMontantsParGenre(Map<String, BigDecimal> montants) {
        return montants.entrySet()
                .stream()
                .map(entree -> new MontantParGenreResponse(entree.getKey(), entree.getValue()))
                .toList();
    }

    private List<MontantParMoisResponse> convertirMontantsParMois(Map<String, BigDecimal> montants) {
        return montants.entrySet()
                .stream()
                .map(entree -> new MontantParMoisResponse(entree.getKey(), entree.getValue()))
                .toList();
    }
}
