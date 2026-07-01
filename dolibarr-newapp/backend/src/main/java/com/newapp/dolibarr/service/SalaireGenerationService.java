package com.newapp.dolibarr.service;

import com.newapp.dolibarr.dto.GenerationSalairesResponse;
import com.newapp.dolibarr.dto.GenererSalairesRequest;
import com.newapp.dolibarr.dto.ResumeGenerationSalairesDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class SalaireGenerationService {

    private static final long ID_SUPERADMIN = 1L;

    private final DolibarrClientService dolibarrClientService;

    public SalaireGenerationService(DolibarrClientService dolibarrClientService) {
        this.dolibarrClientService = dolibarrClientService;
    }

    public GenerationSalairesResponse genererSalaires(GenererSalairesRequest request) {
        String erreurValidation = valider(request);
        if (erreurValidation != null) {
            return new GenerationSalairesResponse(
                    false,
                    erreurValidation,
                    new ResumeGenerationSalairesDto(0, 0, List.of(erreurValidation))
            );
        }

        ResumeGeneration resume = new ResumeGeneration();
        List<Map<String, Object>> salaires = convertirListe(dolibarrClientService.listerRessources("/salaries"));

        for (Long employeId : request.employeIds()) {
            creerSalairePourEmploye(employeId, request, salaires, resume);
        }

        ResumeGenerationSalairesDto dto = creerResumeGeneration(resume);
        boolean success = dto.erreurs().isEmpty();
        return new GenerationSalairesResponse(success, "Salaires generes", dto);
    }

    public void creerSalairePourEmploye(
            Long employeId,
            GenererSalairesRequest request,
            List<Map<String, Object>> salairesExistants,
            ResumeGeneration resume
    ) {
        try {
            if (employeId == null || employeId == ID_SUPERADMIN) {
                resume.salairesIgnores++;
                ajouterErreur(resume, "Employe invalide ignore : " + employeId);
                return;
            }

            Map<String, Object> employe = chargerUtilisateur(employeId);
            if (employe == null) {
                resume.salairesIgnores++;
                ajouterErreur(resume, "Employe introuvable ignore : " + employeId);
                return;
            }
            if (!estActif(employe)) {
                resume.salairesIgnores++;
                ajouterErreur(resume, "Employe inactif ignore : " + libelleEmploye(employeId, employe));
                return;
            }
            if (salaireExiste(employeId, request.dateDebut(), request.dateFin(), salairesExistants)) {
                resume.salairesIgnores++;
                ajouterErreur(resume, "Salaire deja existant ignore : " + libelleEmploye(employeId, employe));
                return;
            }

            Map<String, Object> corps = new LinkedHashMap<>();
            corps.put("fk_user", employeId);
            corps.put("label", "Salaire " + request.dateDebut() + " - " + request.dateFin());
            corps.put("amount", request.montant());
            corps.put("datesp", convertirTimestamp(request.dateDebut()));
            corps.put("dateep", convertirTimestamp(request.dateFin()));
            corps.put("import_key", "NEWAPP");
            corps.put("note_private", "NEWAPP_GENERATION");
            corps.put("ref", "NEWAPP_GENERATION-" + employeId + "-" + request.dateDebut() + "-" + request.dateFin());

            dolibarrClientService.appelerDolibarr("/salaries", HttpMethod.POST, corps, String.class);
            resume.salairesCrees++;
        } catch (Exception exception) {
            ajouterErreur(resume, "Erreur employe " + employeId + " : " + dolibarrClientService.gererErreurDolibarr(exception));
        }
    }

    public ResumeGenerationSalairesDto creerResumeGeneration(ResumeGeneration resume) {
        return new ResumeGenerationSalairesDto(resume.salairesCrees, resume.salairesIgnores, resume.erreurs);
    }

    public void ajouterErreur(ResumeGeneration resume, String erreur) {
        resume.erreurs.add(erreur);
    }

    private String valider(GenererSalairesRequest request) {
        if (request == null || request.employeIds() == null || request.employeIds().isEmpty()) {
            return "Selectionnez au moins un salarie";
        }
        if (request.dateDebut() == null || request.dateDebut().isBlank()
                || request.dateFin() == null || request.dateFin().isBlank()) {
            return "Les dates de debut et de fin sont obligatoires";
        }
        if (request.montant() == null || request.montant() <= 0) {
            return "Le montant doit etre superieur a 0";
        }
        try {
            LocalDate debut = LocalDate.parse(request.dateDebut());
            LocalDate fin = LocalDate.parse(request.dateFin());
            if (fin.isBefore(debut)) {
                return "La date de fin doit etre posterieure ou egale a la date de debut";
            }
        } catch (Exception exception) {
            return "Les dates doivent etre au format yyyy-MM-dd";
        }
        return null;
    }

    private boolean salaireExiste(Long employeId, String dateDebut, String dateFin, List<Map<String, Object>> salaires) {
        for (Map<String, Object> salaire : salaires) {
            if (!employeId.equals(convertirLong(valeur(salaire, "fk_user", "fk_userid", "user_id", "employee_id")))) {
                continue;
            }
            String debut = convertirDateIso(valeur(salaire, "datesp", "date_start", "datedebut"));
            String fin = convertirDateIso(valeur(salaire, "dateep", "date_end", "datefin"));
            if (dateDebut.equals(debut) && dateFin.equals(fin)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> chargerUtilisateur(Long id) {
        try {
            Object body = dolibarrClientService.appelerDolibarr("/users/" + id, HttpMethod.GET, null, Map.class).getBody();
            return convertirMap(body);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean estActif(Map<String, Object> user) {
        Object statut = valeur(user, "statut", "status", "active");
        if (statut == null) {
            return true;
        }
        String texte = statut.toString().trim();
        return "1".equals(texte) || "true".equalsIgnoreCase(texte) || "active".equalsIgnoreCase(texte);
    }

    private String libelleEmploye(Long id, Map<String, Object> user) {
        String nom = texte(valeur(user, "lastname", "nom"));
        return nom.isBlank() ? String.valueOf(id) : nom + " (#" + id + ")";
    }

    private Long convertirTimestamp(String dateIso) {
        return LocalDate.parse(dateIso.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(ZoneOffset.UTC).toEpochSecond();
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

    private Object valeur(Map<String, Object> objet, String... cles) {
        for (String cle : cles) {
            Object valeur = objet.get(cle);
            if (valeur != null && !valeur.toString().isBlank()) {
                return valeur;
            }
        }
        return null;
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

    public static class ResumeGeneration {
        private int salairesCrees;
        private int salairesIgnores;
        private final List<String> erreurs = new ArrayList<>();
    }
}
