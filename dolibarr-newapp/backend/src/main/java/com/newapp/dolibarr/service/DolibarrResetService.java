package com.newapp.dolibarr.service;

import com.newapp.dolibarr.config.DolibarrProperties;
import com.newapp.dolibarr.dto.ReinitialisationResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DolibarrResetService {

    private final DolibarrClientService dolibarrClientService;
    private final DolibarrProperties dolibarrProperties;

    public DolibarrResetService(DolibarrClientService dolibarrClientService, DolibarrProperties dolibarrProperties) {
        this.dolibarrClientService = dolibarrClientService;
        this.dolibarrProperties = dolibarrProperties;
    }

    public ReinitialisationResponse reinitialiserDonnees() {
        List<String> errors = new ArrayList<>();
        int paymentsDeleted = supprimerPaiements(errors);
        int salariesDeleted = supprimerSalaires(errors);
        ResumeUtilisateurs resumeUtilisateurs = supprimerUtilisateursImportes(errors);

        return creerResumeReset(
                resumeUtilisateurs.usersDeleted(),
                resumeUtilisateurs.usersSkippedProtected(),
                resumeUtilisateurs.usersSkippedNotNewApp(),
                salariesDeleted,
                paymentsDeleted,
                errors
        );
    }

    public ResumeUtilisateurs supprimerUtilisateursImportes(List<String> errors) {
        int usersDeleted = 0;
        int usersSkippedProtected = 0;
        int usersSkippedNotNewApp = 0;

        try {
            List<?> utilisateurs = dolibarrClientService.listerRessources("/users");

            if (utilisateurs == null || utilisateurs.isEmpty()) {
                return new ResumeUtilisateurs(0, 0, 0);
            }

            for (Object utilisateur : utilisateurs) {
                Long id = dolibarrClientService.extraireId(utilisateur);
                Object utilisateurComplet = chargerDetailsUtilisateur(utilisateur, id, errors);

                if (utilisateurComplet == null) {
                    usersSkippedNotNewApp++;
                    continue;
                }

                if (utilisateurProtege(utilisateurComplet)) {
                    usersSkippedProtected++;
                    continue;
                }

                if (!utilisateurImporteNewApp(utilisateurComplet)) {
                    usersSkippedNotNewApp++;
                    continue;
                }

                if (id == null) {
                    ajouterErreur(errors, "Utilisateur NewApp sans id détecté, suppression ignorée");
                    continue;
                }

                try {
                    dolibarrClientService.supprimerRessource("/users", id);
                    usersDeleted++;
                } catch (Exception exception) {
                    ajouterErreur(errors, "Erreur suppression utilisateur " + id + " : " + dolibarrClientService.gererErreurDolibarr(exception));
                }
            }
        } catch (Exception exception) {
            ajouterErreur(errors, "Erreur liste utilisateurs : " + dolibarrClientService.gererErreurDolibarr(exception));
        }

        return new ResumeUtilisateurs(usersDeleted, usersSkippedProtected, usersSkippedNotNewApp);
    }

    public Object chargerDetailsUtilisateur(Object utilisateur, Long id, List<String> errors) {
        if (id == null) {
            return utilisateur;
        }

        try {
            return dolibarrClientService.appelerDolibarr("/users/" + id, org.springframework.http.HttpMethod.GET, null, Map.class).getBody();
        } catch (Exception exception) {
            ajouterErreur(errors, "Impossible de vérifier l'utilisateur " + id + " : " + dolibarrClientService.gererErreurDolibarr(exception));
            return null;
        }
    }

    public boolean utilisateurProtege(Object utilisateur) {
        Long id = dolibarrClientService.extraireId(utilisateur);
        String login = valeurTexte(utilisateur, "login");
        Integer admin = valeurEntier(utilisateur, "admin");

        if (id != null && dolibarrProperties.getProtectedUserIds().contains(id)) {
            return true;
        }

        if (login != null && contientLoginProtege(login)) {
            return true;
        }

        return admin != null && admin == 1;
    }

    public boolean utilisateurImporteNewApp(Object utilisateur) {
        String login = valeurTexte(utilisateur, "login");
        String notePrivate = valeurTexte(utilisateur, "note_private");
        String importKey = valeurTexte(utilisateur, "import_key");

        return login != null && login.startsWith("newapp_")
                || notePrivate != null && notePrivate.contains("NEWAPP_IMPORT")
                || "NEWAPP".equals(importKey);
    }

    public int supprimerSalaires(List<String> errors) {
        try {
            return dolibarrClientService.supprimerRessources("/salaries", "/salaries/salary");
        } catch (Exception exception) {
            ajouterErreur(errors, "Erreur suppression salaires : " + dolibarrClientService.gererErreurDolibarr(exception));
            return 0;
        }
    }

    public int supprimerPaiements(List<String> errors) {
        try {
            return dolibarrClientService.supprimerRessources("/salaries/payments", "/salaries/{id}/payments");
        } catch (Exception exception) {
            ajouterErreur(errors, "Erreur suppression paiements : " + dolibarrClientService.gererErreurDolibarr(exception));
            return 0;
        }
    }

    public void ajouterErreur(List<String> errors, String erreur) {
        errors.add(erreur);
    }

    public ReinitialisationResponse creerResumeReset(
            int usersDeleted,
            int usersSkippedProtected,
            int usersSkippedNotNewApp,
            int salariesDeleted,
            int paymentsDeleted,
            List<String> errors
    ) {
        boolean success = errors.isEmpty();

        return new ReinitialisationResponse(
                success,
                success ? "Données Dolibarr réinitialisées" : "Réinitialisation terminée avec erreurs",
                usersDeleted,
                usersSkippedProtected,
                usersSkippedNotNewApp,
                salariesDeleted,
                paymentsDeleted,
                errors
        );
    }

    private boolean contientLoginProtege(String login) {
        String loginNormalise = login.toLowerCase(Locale.ROOT);

        return dolibarrProperties.getProtectedUserLogins()
                .stream()
                .anyMatch(loginProtege -> loginNormalise.equals(loginProtege.toLowerCase(Locale.ROOT)));
    }

    private String valeurTexte(Object ressource, String cle) {
        if (!(ressource instanceof Map<?, ?> donnees)) {
            return null;
        }

        Object valeur = donnees.get(cle);
        return valeur == null ? null : valeur.toString();
    }

    private Integer valeurEntier(Object ressource, String cle) {
        if (!(ressource instanceof Map<?, ?> donnees)) {
            return null;
        }

        Object valeur = donnees.get(cle);

        if (valeur instanceof Number nombre) {
            return nombre.intValue();
        }

        if (valeur instanceof String texte && !texte.isBlank()) {
            return Integer.parseInt(texte);
        }

        return null;
    }

    public record ResumeUtilisateurs(
            int usersDeleted,
            int usersSkippedProtected,
            int usersSkippedNotNewApp
    ) {
    }
}
