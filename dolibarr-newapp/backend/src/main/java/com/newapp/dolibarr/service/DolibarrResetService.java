package com.newapp.dolibarr.service;

import com.newapp.dolibarr.config.DolibarrProperties;
import com.newapp.dolibarr.dto.ReinitialisationResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpMethod;
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
        ResumeUtilisateurs resumeUtilisateurs = supprimerUtilisateursNonProteges(errors);

        return creerResumeReset(
                resumeUtilisateurs.usersDeleted(),
                resumeUtilisateurs.usersSkippedProtected(),
                resumeUtilisateurs.usersSkippedNotNewApp(),
                salariesDeleted,
                paymentsDeleted,
                errors
        );
    }

    public ResumeUtilisateurs supprimerUtilisateursNonProteges(List<String> errors) {
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
                    ajouterErreur(errors, "Utilisateur " + id + " impossible a verifier, suppression ignoree");
                    continue;
                }

                if (utilisateurProtege(utilisateurComplet)) {
                    usersSkippedProtected++;
                    continue;
                }

                if (id == null) {
                    ajouterErreur(errors, "Utilisateur sans id detecte, suppression ignoree");
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
            return dolibarrClientService.appelerDolibarr("/users/" + id, HttpMethod.GET, null, Map.class).getBody();
        } catch (Exception exception) {
            ajouterErreur(errors, "Impossible de verifier l'utilisateur " + id + " : " + dolibarrClientService.gererErreurDolibarr(exception));
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
                success ? "Donnees Dolibarr reinitialisees" : "Reinitialisation terminee avec erreurs",
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
            try {
                return Integer.parseInt(texte);
            } catch (NumberFormatException exception) {
                return null;
            }
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
