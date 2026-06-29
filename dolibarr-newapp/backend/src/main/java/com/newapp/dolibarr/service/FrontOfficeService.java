package com.newapp.dolibarr.service;

import com.newapp.dolibarr.dto.CreerPaiementRequest;
import com.newapp.dolibarr.dto.CreerSalaireRequest;
import com.newapp.dolibarr.dto.EmployeListeDto;
import com.newapp.dolibarr.dto.ModePaiementDto;
import com.newapp.dolibarr.dto.OperationResponse;
import com.newapp.dolibarr.dto.PaiementDetailDto;
import com.newapp.dolibarr.dto.SalaireDetailDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

/**
 * Logique du front-office : liste des salariés avec agrégats, détail d'un salaire,
 * création de salaires et de paiements partiels. Dolibarr reste la seule source de
 * vérité ; rien n'est mis en cache, tout est relu à chaque appel.
 *
 * Garde-fou transverse : le superadmin (ID 1) est exclu des listes et ne peut jamais
 * recevoir de salaire ni de paiement.
 */
@Service
public class FrontOfficeService {

    private static final long ID_SUPERADMIN = 1L;

    // Modes de paiement actifs (c_paiement) confirmés sur l'instance, exposés au formulaire.
    private static final Map<Integer, String> MODES_PAIEMENT = new LinkedHashMap<>();

    static {
        MODES_PAIEMENT.put(2, "Virement");
        MODES_PAIEMENT.put(4, "Espèces");
        MODES_PAIEMENT.put(7, "Chèque");
        MODES_PAIEMENT.put(6, "Carte");
        MODES_PAIEMENT.put(3, "Prélèvement");
    }

    private final DolibarrClientService dolibarrClientService;

    public FrontOfficeService(DolibarrClientService dolibarrClientService) {
        this.dolibarrClientService = dolibarrClientService;
    }

    // ─────────────────────────── Modes de paiement ───────────────────────────

    public List<ModePaiementDto> listerModesPaiement() {
        List<ModePaiementDto> modes = new ArrayList<>();
        MODES_PAIEMENT.forEach((id, libelle) -> modes.add(new ModePaiementDto(id, libelle)));
        return modes;
    }

    // ─────────────────────────── Liste des salariés ───────────────────────────

    public List<EmployeListeDto> listerEmployes() {
        List<Map<String, Object>> users = convertirListe(dolibarrClientService.listerRessources("/users"));
        List<Map<String, Object>> salaires = convertirListe(dolibarrClientService.listerRessources("/salaries"));
        List<Map<String, Object>> paiements = convertirListe(dolibarrClientService.listerRessources("/salaries/payments"));

        Map<Long, BigDecimal> payeParSalaire = totalPayeParSalaire(paiements);

        List<EmployeListeDto> resultat = new ArrayList<>();
        for (Map<String, Object> user : users) {
            Long idUser = dolibarrClientService.extraireId(user);
            if (idUser == null || idUser == ID_SUPERADMIN) {
                continue; // garde-fou : jamais le superadmin
            }

            int nbSalaires = 0;
            BigDecimal montantTotal = BigDecimal.ZERO;
            BigDecimal totalPaye = BigDecimal.ZERO;

            for (Map<String, Object> salaire : salaires) {
                if (!idUser.equals(idUtilisateurDuSalaire(salaire))) {
                    continue;
                }
                Long idSalaire = dolibarrClientService.extraireId(salaire);
                BigDecimal montant = convertirMontant(getValeur(salaire, "amount", "montant"));
                nbSalaires++;
                montantTotal = montantTotal.add(montant);
                totalPaye = totalPaye.add(payeParSalaire.getOrDefault(idSalaire, BigDecimal.ZERO));
            }

            resultat.add(new EmployeListeDto(
                    idUser,
                    texte(getValeur(user, "lastname", "nom")),
                    texte(getValeur(user, "login")),
                    texte(getValeur(user, "gender", "genre")),
                    convertirDouble(getValeur(user, "weeklyhours")),
                    nbSalaires,
                    montantTotal,
                    montantTotal.subtract(totalPaye)
            ));
        }

        resultat.sort(Comparator.comparing(employe -> texte(employe.nom()).toLowerCase()));
        return resultat;
    }

    // ─────────────────────────── Salaires d'un employé ───────────────────────────

    public List<SalaireDetailDto> listerSalairesEmploye(Long idEmploye) {
        List<Map<String, Object>> salaires = convertirListe(dolibarrClientService.listerRessources("/salaries"));
        List<Map<String, Object>> paiements = convertirListe(dolibarrClientService.listerRessources("/salaries/payments"));
        Map<String, Object> user = chargerUtilisateur(idEmploye);

        List<SalaireDetailDto> resultat = new ArrayList<>();
        for (Map<String, Object> salaire : salaires) {
            if (idEmploye.equals(idUtilisateurDuSalaire(salaire))) {
                resultat.add(construireDetail(salaire, user, paiements));
            }
        }
        return resultat;
    }

    // ─────────────────────────── Détail d'un salaire ───────────────────────────

    public SalaireDetailDto detailSalaire(Long idSalaire) {
        Map<String, Object> salaire = chargerSalaire(idSalaire);
        if (salaire == null) {
            return null;
        }
        Long idUser = idUtilisateurDuSalaire(salaire);
        Map<String, Object> user = idUser == null ? null : chargerUtilisateur(idUser);
        List<Map<String, Object>> paiements = convertirListe(dolibarrClientService.listerRessources("/salaries/payments"));
        return construireDetail(salaire, user, paiements);
    }

    // ─────────────────────────── Création d'un salaire ───────────────────────────

    public OperationResponse creerSalaire(CreerSalaireRequest requete) {
        if (requete.fkUser() == null || requete.fkUser() == ID_SUPERADMIN) {
            return OperationResponse.echec("Employé invalide (le superadmin est interdit)");
        }
        if (requete.montant() == null || requete.montant() <= 0) {
            return OperationResponse.echec("Le montant doit être supérieur à 0");
        }
        if (requete.dateDebut() == null || requete.dateFin() == null) {
            return OperationResponse.echec("Les dates de début et de fin sont obligatoires");
        }
        if (requete.dateFin().compareTo(requete.dateDebut()) < 0) {
            return OperationResponse.echec("La date de fin doit être postérieure ou égale à la date de début");
        }

        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("fk_user", requete.fkUser());
        corps.put("label", "Salaire du " + requete.dateDebut());
        corps.put("amount", requete.montant());
        corps.put("datesp", convertirTimestamp(requete.dateDebut()));
        corps.put("dateep", convertirTimestamp(requete.dateFin()));

        try {
            var reponse = dolibarrClientService.appelerDolibarr("/salaries", HttpMethod.POST, corps, String.class);
            Long id = extraireIdReponse(reponse.getBody());
            return OperationResponse.ok("Salaire créé", id);
        } catch (Exception exception) {
            return OperationResponse.echec(dolibarrClientService.gererErreurDolibarr(exception));
        }
    }

    // ─────────────────────────── Paiement partiel ───────────────────────────

    public OperationResponse ajouterPaiement(Long idSalaire, CreerPaiementRequest requete) {
        SalaireDetailDto detail = detailSalaire(idSalaire);
        if (detail == null) {
            return OperationResponse.echec("Salaire introuvable");
        }
        if (detail.idEmploye() != null && detail.idEmploye() == ID_SUPERADMIN) {
            return OperationResponse.echec("Paiement interdit pour le superadmin");
        }
        if (requete.montant() == null || requete.montant() <= 0) {
            return OperationResponse.echec("Le montant doit être supérieur à 0");
        }

        BigDecimal montant = BigDecimal.valueOf(requete.montant());
        if (montant.compareTo(detail.resteAPayer()) > 0) {
            return OperationResponse.echec("Le montant dépasse le reste à payer (" + detail.resteAPayer() + ")");
        }

        int fkType = requete.fkTypePayment() == null ? 0 : requete.fkTypePayment();

        // Contrat vérifié sur la source Dolibarr : amounts est une map { idSalaire : montant },
        // chid + paiementtype + fk_typepayment requis, date en timestamp.
        Map<String, Object> amounts = new LinkedHashMap<>();
        amounts.put(String.valueOf(idSalaire), requete.montant());

        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("chid", idSalaire);
        corps.put("datepaye", convertirTimestamp(requete.date()));
        corps.put("paiementtype", fkType);
        corps.put("fk_typepayment", fkType);
        corps.put("amounts", amounts);

        try {
            var reponse = dolibarrClientService.appelerDolibarr(
                    "/salaries/" + idSalaire + "/payments", HttpMethod.POST, corps, String.class);
            Long id = extraireIdReponse(reponse.getBody());
            return OperationResponse.ok("Paiement enregistré", id);
        } catch (Exception exception) {
            return OperationResponse.echec(dolibarrClientService.gererErreurDolibarr(exception));
        }
    }

    // ─────────────────────────── Photo employé ───────────────────────────

    /** Renvoie le contenu binaire de la photo d'un employé, ou null si absente / superadmin. */
    public FichierBinaire photoEmploye(Long idUser) {
        if (idUser == null || idUser == ID_SUPERADMIN) {
            return null;
        }

        for (String nom : List.of("photo.png", "photo.jpg")) {
            Map<String, Object> document = dolibarrClientService.telechargerDocument("user", idUser + "/photos/" + nom);
            if (document != null && document.get("content") != null) {
                byte[] contenu = Base64.getDecoder().decode(document.get("content").toString());
                String mime = document.get("content-type") != null
                        ? document.get("content-type").toString()
                        : (nom.endsWith(".jpg") ? "image/jpeg" : "image/png");
                return new FichierBinaire(contenu, mime);
            }
        }
        return null;
    }

    public record FichierBinaire(byte[] contenu, String typeMime) {
    }

    // ─────────────────────────── Construction / calculs ───────────────────────────

    private SalaireDetailDto construireDetail(Map<String, Object> salaire, Map<String, Object> user, List<Map<String, Object>> paiements) {
        Long idSalaire = dolibarrClientService.extraireId(salaire);
        BigDecimal montant = convertirMontant(getValeur(salaire, "amount", "montant"));

        List<PaiementDetailDto> historique = new ArrayList<>();
        BigDecimal totalPaye = BigDecimal.ZERO;
        for (Map<String, Object> paiement : paiements) {
            if (!idSalaire.equals(idSalaireDuPaiement(paiement))) {
                continue;
            }
            BigDecimal montantPaiement = convertirMontant(getValeur(paiement,
                    "amount", "amount_payment", "montant", "total_ttc"));
            totalPaye = totalPaye.add(montantPaiement);
            historique.add(new PaiementDetailDto(
                    convertirDateIso(getValeur(paiement, "datep", "date_payment", "date_paid", "date_reglement", "datepaye", "date")),
                    montantPaiement,
                    libelleMode(getValeur(paiement, "fk_typepayment", "type_payment", "paiementtype"))
            ));
        }
        historique.sort(Comparator.comparing(paiement -> texte(paiement.date())));

        BigDecimal reste = montant.subtract(totalPaye);

        return new SalaireDetailDto(
                idSalaire,
                user == null ? idUtilisateurDuSalaire(salaire) : dolibarrClientService.extraireId(user),
                user == null ? null : texte(getValeur(user, "lastname", "nom")),
                user == null ? null : texte(getValeur(user, "login")),
                user == null ? null : texte(getValeur(user, "gender", "genre")),
                montant,
                convertirDateIso(getValeur(salaire, "datesp", "date_start", "datedebut")),
                convertirDateIso(getValeur(salaire, "dateep", "date_end", "datefin")),
                totalPaye,
                reste,
                calculerStatut(reste, totalPaye),
                historique
        );
    }

    private String calculerStatut(BigDecimal reste, BigDecimal totalPaye) {
        if (reste.signum() <= 0) {
            return "solde";
        }
        return totalPaye.signum() > 0 ? "partiel" : "impaye";
    }

    private Map<Long, BigDecimal> totalPayeParSalaire(List<Map<String, Object>> paiements) {
        Map<Long, BigDecimal> total = new HashMap<>();
        for (Map<String, Object> paiement : paiements) {
            Long idSalaire = idSalaireDuPaiement(paiement);
            if (idSalaire == null) {
                continue;
            }
            BigDecimal montant = convertirMontant(getValeur(paiement, "amount", "amount_payment", "montant", "total_ttc"));
            total.merge(idSalaire, montant, BigDecimal::add);
        }
        return total;
    }

    private String libelleMode(Object valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            int id = Integer.parseInt(valeur.toString().trim());
            return MODES_PAIEMENT.getOrDefault(id, "Mode " + id);
        } catch (NumberFormatException exception) {
            return valeur.toString();
        }
    }

    // ─────────────────────────── Accès Dolibarr ───────────────────────────

    private Map<String, Object> chargerUtilisateur(Long idUser) {
        try {
            Object body = dolibarrClientService.appelerDolibarr("/users/" + idUser, HttpMethod.GET, null, Map.class).getBody();
            return convertirMap(body);
        } catch (Exception exception) {
            return null;
        }
    }

    private Map<String, Object> chargerSalaire(Long idSalaire) {
        try {
            Object body = dolibarrClientService.appelerDolibarr("/salaries/" + idSalaire, HttpMethod.GET, null, Map.class).getBody();
            return convertirMap(body);
        } catch (Exception exception) {
            return null;
        }
    }

    private Long idUtilisateurDuSalaire(Map<String, Object> salaire) {
        return convertirLong(getValeur(salaire, "fk_user", "fk_userid", "user_id", "employee_id"));
    }

    private Long idSalaireDuPaiement(Map<String, Object> paiement) {
        return convertirLong(getValeur(paiement, "fk_salary", "salary_id", "id_salary", "fk_object"));
    }

    // ─────────────────────────── Outils de conversion ───────────────────────────

    private Object getValeur(Map<String, Object> objet, String... cles) {
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

    private String texte(Object valeur) {
        return valeur == null ? "" : valeur.toString();
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

    /** Convertit une valeur date Dolibarr (timestamp epoch ou chaîne) en ISO yyyy-MM-dd. */
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
                // format inattendu : on renvoie tel quel
            }
        }
        return texte;
    }

    /** Convertit une date ISO yyyy-MM-dd en timestamp Unix (secondes) attendu par Dolibarr. */
    private Long convertirTimestamp(String dateIso) {
        if (dateIso == null || dateIso.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateIso.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        } catch (Exception exception) {
            return null;
        }
    }

    private Long extraireIdReponse(String corps) {
        if (corps == null || corps.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(corps.trim().replace("\"", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
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
}
