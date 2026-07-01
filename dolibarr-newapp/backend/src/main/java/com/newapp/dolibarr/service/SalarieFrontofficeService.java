package com.newapp.dolibarr.service;

import com.newapp.dolibarr.dto.DetailSalarieFrontofficeDto;
import com.newapp.dolibarr.dto.PaiementHistoriqueDto;
import com.newapp.dolibarr.dto.SalaireHistoriqueDto;
import com.newapp.dolibarr.dto.SalarieFrontofficeDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class SalarieFrontofficeService {

    private static final long ID_SUPERADMIN = 1L;

    private final DolibarrClientService dolibarrClientService;

    public SalarieFrontofficeService(DolibarrClientService dolibarrClientService) {
        this.dolibarrClientService = dolibarrClientService;
    }

    public List<SalarieFrontofficeDto> recupererTousLesSalaries() {
        List<SalarieFrontofficeDto> salaries = new ArrayList<>();
        for (Map<String, Object> user : convertirListe(dolibarrClientService.listerRessources("/users"))) {
            if (estSalarieAffichable(user)) {
                salaries.add(convertirSalarie(user));
            }
        }
        salaries.sort(Comparator.comparing(salarie -> texte(salarie.nom()).toLowerCase()));
        return salaries;
    }

    public DetailSalarieFrontofficeDto recupererDetailSalarie(Long id) {
        Map<String, Object> user = chargerUtilisateur(id);
        if (user == null || !estSalarieAffichable(user)) {
            return new DetailSalarieFrontofficeDto(null, List.of());
        }

        return new DetailSalarieFrontofficeDto(
                convertirSalarie(user),
                recupererSalairesDuSalarie(id)
        );
    }

    public List<SalaireHistoriqueDto> recupererSalairesDuSalarie(Long idSalarie) {
        List<Map<String, Object>> paiements = convertirListe(dolibarrClientService.listerRessources("/salaries/payments"));
        List<SalaireHistoriqueDto> historiques = new ArrayList<>();

        for (Map<String, Object> salaire : convertirListe(dolibarrClientService.listerRessources("/salaries"))) {
            if (idSalarie.equals(idUtilisateurDuSalaire(salaire))) {
                historiques.add(convertirSalaire(salaire, paiements));
            }
        }

        historiques.sort(Comparator.comparing(salaire -> texte(salaire.dateDebut())));
        return historiques;
    }

    public List<PaiementHistoriqueDto> recupererPaiementsDuSalaire(Long idSalaire, List<Map<String, Object>> paiements) {
        List<PaiementHistoriqueDto> resultat = new ArrayList<>();
        Set<String> idsDejaVus = new HashSet<>();

        for (Map<String, Object> paiement : paiements) {
            if (!idSalaire.equals(idSalaireDuPaiement(paiement))) {
                continue;
            }
            Long idPaiement = dolibarrClientService.extraireId(paiement);
            String cle = idPaiement == null ? paiement.toString() : idPaiement.toString();
            if (!idsDejaVus.add(cle)) {
                continue;
            }
            resultat.add(convertirPaiement(paiement));
        }

        resultat.sort(Comparator.comparing(paiement -> texte(paiement.datePaiement())));
        return resultat;
    }

    public BigDecimal calculerTotalPaye(List<PaiementHistoriqueDto> paiements) {
        BigDecimal total = BigDecimal.ZERO;
        for (PaiementHistoriqueDto paiement : paiements) {
            total = total.add(paiement.montantPaiement());
        }
        return total;
    }

    public BigDecimal calculerResteAPayer(BigDecimal montantSalaire, BigDecimal totalPaye) {
        return montantSalaire.subtract(totalPaye);
    }

    public String determinerStatutPaiement(BigDecimal totalPaye, BigDecimal resteAPayer) {
        if (resteAPayer.signum() <= 0) {
            return "Paye";
        }
        if (totalPaye.signum() > 0) {
            return "Partiel";
        }
        return "Non paye";
    }

    public SalarieFrontofficeDto convertirSalarie(Map<String, Object> user) {
        return new SalarieFrontofficeDto(
                dolibarrClientService.extraireId(user),
                texte(valeur(user, "ref", "ref_employee", "employee_ref", "rowid", "id")),
                texte(valeur(user, "lastname", "nom")),
                texte(valeur(user, "firstname", "prenom", "forename")),
                texte(valeur(user, "login")),
                texte(valeur(user, "job", "poste")),
                convertirGenre(texte(valeur(user, "gender", "genre"))),
                convertirDouble(valeur(user, "weeklyhours")),
                estActif(user) ? "Actif" : "Inactif"
        );
    }

    public SalaireHistoriqueDto convertirSalaire(Map<String, Object> salaire, List<Map<String, Object>> paiementsBruts) {
        Long idSalaire = dolibarrClientService.extraireId(salaire);
        BigDecimal montantSalaire = convertirMontant(valeur(salaire, "amount", "montant"));
        List<PaiementHistoriqueDto> paiements = recupererPaiementsDuSalaire(idSalaire, paiementsBruts);
        BigDecimal totalPaye = calculerTotalPaye(paiements);
        BigDecimal resteAPayer = calculerResteAPayer(montantSalaire, totalPaye);

        return new SalaireHistoriqueDto(
                idSalaire,
                texte(valeur(salaire, "ref", "label", "rowid", "id")),
                convertirDateIso(valeur(salaire, "datesp", "date_start", "datedebut")),
                convertirDateIso(valeur(salaire, "dateep", "date_end", "datefin")),
                montantSalaire,
                totalPaye,
                resteAPayer,
                determinerStatutPaiement(totalPaye, resteAPayer),
                paiements
        );
    }

    public PaiementHistoriqueDto convertirPaiement(Map<String, Object> paiement) {
        Long idPaiement = dolibarrClientService.extraireId(paiement);
        return new PaiementHistoriqueDto(
                idPaiement,
                convertirDateIso(valeur(paiement, "datep", "date_payment", "date_paid", "date_reglement", "datepaye", "date")),
                convertirMontant(valeur(paiement, "amount", "amount_payment", "montant", "total_ttc")),
                texte(valeur(paiement, "ref", "ref_payment", "num_payment", "rowid", "id"))
        );
    }

    private Map<String, Object> chargerUtilisateur(Long id) {
        if (id == null) {
            return null;
        }
        try {
            Object body = dolibarrClientService.appelerDolibarr("/users/" + id, HttpMethod.GET, null, Map.class).getBody();
            return convertirMap(body);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean estSalarieAffichable(Map<String, Object> user) {
        Long id = dolibarrClientService.extraireId(user);
        if (id == null || id == ID_SUPERADMIN || estAdmin(user)) {
            return false;
        }
        return estActif(user) && estNewApp(user);
    }

    private boolean estNewApp(Map<String, Object> user) {
        String login = texte(valeur(user, "login")).toLowerCase();
        String importKey = texte(valeur(user, "import_key")).toUpperCase();
        String notePrivate = texte(valeur(user, "note_private")).toUpperCase();
        return login.startsWith("newapp_")
                || "NEWAPP".equals(importKey)
                || notePrivate.contains("NEWAPP_IMPORT");
    }

    private boolean estAdmin(Map<String, Object> user) {
        Object admin = valeur(user, "admin", "admin_level", "rights_admin");
        if (admin == null) {
            return false;
        }
        String texte = admin.toString().trim();
        return "1".equals(texte) || "true".equalsIgnoreCase(texte);
    }

    private boolean estActif(Map<String, Object> user) {
        Object statut = valeur(user, "statut", "status", "active");
        if (statut == null) {
            return true;
        }
        String texte = statut.toString().trim();
        return "1".equals(texte) || "true".equalsIgnoreCase(texte) || "active".equalsIgnoreCase(texte);
    }

    private Long idUtilisateurDuSalaire(Map<String, Object> salaire) {
        return convertirLong(valeur(salaire, "fk_user", "fk_userid", "user_id", "employee_id"));
    }

    private Long idSalaireDuPaiement(Map<String, Object> paiement) {
        return convertirLong(valeur(paiement, "fk_salary", "salary_id", "id_salary", "fk_object"));
    }

    private Object valeur(Map<String, Object> objet, String... cles) {
        if (objet == null) {
            return null;
        }
        for (String cle : cles) {
            Object valeur = objet.get(cle);
            if (valeur != null && !valeur.toString().isBlank()) {
                return valeur;
            }
        }
        return null;
    }

    private String convertirGenre(String genre) {
        if ("man".equalsIgnoreCase(genre) || "homme".equalsIgnoreCase(genre)) {
            return "homme";
        }
        if ("woman".equalsIgnoreCase(genre) || "femme".equalsIgnoreCase(genre)) {
            return "femme";
        }
        return genre;
    }

    private BigDecimal convertirMontant(Object valeur) {
        if (valeur == null) {
            return BigDecimal.ZERO;
        }
        if (valeur instanceof Number nombre) {
            return BigDecimal.valueOf(nombre.doubleValue());
        }
        String texte = valeur.toString().trim().replace(" ", "").replace(",", ".");
        try {
            return texte.isBlank() ? BigDecimal.ZERO : new BigDecimal(texte);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
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

    private Long convertirLong(Object valeur) {
        if (valeur == null) {
            return null;
        }
        if (valeur instanceof Number nombre) {
            return nombre.longValue();
        }
        try {
            return Long.parseLong(valeur.toString().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String convertirDateIso(Object valeur) {
        if (valeur == null) {
            return null;
        }
        if (valeur instanceof Number nombre) {
            long timestamp = nombre.longValue();
            if (timestamp > 9_999_999_999L) {
                timestamp = timestamp / 1000;
            }
            return Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC).toLocalDate().toString();
        }
        String texte = valeur.toString().trim();
        if (texte.length() >= 10) {
            try {
                return LocalDate.parse(texte.substring(0, 10)).toString();
            } catch (Exception ignore) {
                return texte;
            }
        }
        return texte;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertirListe(List<?> ressources) {
        List<Map<String, Object>> resultat = new ArrayList<>();
        if (ressources == null) {
            return resultat;
        }
        for (Object ressource : ressources) {
            if (ressource instanceof Map<?, ?> map) {
                resultat.add((Map<String, Object>) map);
            }
        }
        return resultat;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertirMap(Object body) {
        return body instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private String texte(Object valeur) {
        return valeur == null ? "" : valeur.toString();
    }
}
