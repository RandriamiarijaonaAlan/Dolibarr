package com.newapp.dolibarr.service;

import com.newapp.dolibarr.config.DolibarrProperties;
import com.newapp.dolibarr.dto.DolibarrConnectionResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DolibarrClientService {

    private static final Logger logger = LoggerFactory.getLogger(DolibarrClientService.class);

    private final DolibarrProperties dolibarrProperties;
    private final RestTemplate restTemplate;

    public DolibarrClientService(DolibarrProperties dolibarrProperties, RestTemplateBuilder restTemplateBuilder) {
        this.dolibarrProperties = dolibarrProperties;
        this.restTemplate = restTemplateBuilder
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public DolibarrConnectionResponse testConnection() {
        String baseUrl = dolibarrProperties.getBaseUrl();

        try {
            ResponseEntity<String> response = appelerDolibarr("/users/info", HttpMethod.GET, null, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return DolibarrConnectionResponse.success(baseUrl);
            }

            return DolibarrConnectionResponse.error(
                    baseUrl,
                    "Réponse HTTP inattendue : " + response.getStatusCode()
            );
        } catch (HttpStatusCodeException exception) {
            return DolibarrConnectionResponse.error(
                    baseUrl,
                    "Erreur HTTP " + exception.getStatusCode().value() + " : " + exception.getStatusText()
            );
        } catch (ResourceAccessException exception) {
            return DolibarrConnectionResponse.error(
                    baseUrl,
                    "Dolibarr inaccessible : " + exception.getMessage()
            );
        } catch (IllegalArgumentException exception) {
            return DolibarrConnectionResponse.error(
                    baseUrl,
                    "URL Dolibarr invalide : " + exception.getMessage()
            );
        } catch (RestClientException exception) {
            return DolibarrConnectionResponse.error(
                    baseUrl,
                    exception.getMessage()
            );
        }
    }

    public <T> ResponseEntity<T> appelerDolibarr(
            String chemin,
            HttpMethod methode,
            Object corps,
            Class<T> typeReponse
    ) {
        String url = construireUrl(chemin);

        logger.info("Appel Dolibarr : methode={} url={}", methode, url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("DOLAPIKEY", dolibarrProperties.getApiKey());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<T> reponse = restTemplate.exchange(
                    url,
                    methode,
                    new HttpEntity<>(corps, headers),
                    typeReponse
            );

            logger.info("Reponse Dolibarr : methode={} url={} status={}", methode, url, reponse.getStatusCode());

            return reponse;
        } catch (Exception exception) {
            String details = gererErreurDolibarr(exception);
            logger.error("Erreur Dolibarr : methode={} url={} details={}", methode, url, details, exception);
            throw exception;
        }
    }

    public String gererErreurDolibarr(Exception exception) {
        if (exception instanceof HttpStatusCodeException httpException) {
            String corps = httpException.getResponseBodyAsString();
            String details = corps == null || corps.isBlank() ? httpException.getStatusText() : corps;
            return "Erreur HTTP " + httpException.getStatusCode().value() + " : " + details;
        }

        if (exception instanceof ResourceAccessException) {
            return "Dolibarr inaccessible : " + exception.getMessage();
        }

        if (exception instanceof IllegalArgumentException) {
            return "URL Dolibarr invalide : " + exception.getMessage();
        }

        if (exception instanceof RestClientException) {
            return "Erreur Dolibarr : " + exception.getMessage();
        }

        return "Erreur inattendue : " + exception.getMessage();
    }

    public int supprimerRessources(String cheminListe, String cheminSuppression) {
        List<?> ressources = listerRessources(cheminListe);

        if (ressources == null || ressources.isEmpty()) {
            return 0;
        }

        int totalSupprime = 0;
        for (Object ressource : ressources) {
            Long id = extraireId(ressource);
            if (id != null) {
                appelerDolibarr(construireCheminSuppression(cheminSuppression, id), HttpMethod.DELETE, null, String.class);
                totalSupprime++;
            }
        }

        return totalSupprime;
    }

    public List<?> listerRessources(String cheminListe) {
        ResponseEntity<List> reponse = appelerDolibarr(cheminListe, HttpMethod.GET, null, List.class);
        return reponse.getBody();
    }

    public void supprimerRessource(String cheminSuppression, Long id) {
        appelerDolibarr(construireCheminSuppression(cheminSuppression, id), HttpMethod.DELETE, null, String.class);
    }

    /**
     * Vérifie qu'un fichier d'un module est accessible via GET /documents/download.
     * Les paramètres sont passés en query (et non dans le chemin) car original_file contient
     * des slashes. Renvoie vrai si Dolibarr répond 2xx, faux sinon (404, erreur, etc.).
     */
    public boolean fichierAccessible(String modulepart, String originalFile) {
        String url = UriComponentsBuilder
                .fromHttpUrl(dolibarrProperties.getBaseUrl())
                .path("/documents/download")
                .queryParam("modulepart", modulepart)
                .queryParam("original_file", originalFile)
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("DOLAPIKEY", dolibarrProperties.getApiKey());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<String> reponse = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return reponse.getStatusCode().is2xxSuccessful();
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Télécharge un document via GET /documents/download et renvoie la réponse Dolibarr
     * (filename, content-type, content en base64...), ou null si le fichier est introuvable.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> telechargerDocument(String modulepart, String originalFile) {
        String url = UriComponentsBuilder
                .fromHttpUrl(dolibarrProperties.getBaseUrl())
                .path("/documents/download")
                .queryParam("modulepart", modulepart)
                .queryParam("original_file", originalFile)
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("DOLAPIKEY", dolibarrProperties.getApiKey());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<Map> reponse = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return reponse.getBody();
        } catch (Exception exception) {
            return null;
        }
    }

    private String construireUrl(String chemin) {
        String cheminNormalise = chemin.startsWith("/") ? chemin : "/" + chemin;

        return UriComponentsBuilder
                .fromHttpUrl(dolibarrProperties.getBaseUrl())
                .path(cheminNormalise)
                .toUriString();
    }

    private String construireCheminSuppression(String cheminSuppression, Long id) {
        if (cheminSuppression.contains("{id}")) {
            return cheminSuppression.replace("{id}", id.toString());
        }

        return cheminSuppression + "/" + id;
    }

    public Long extraireId(Object ressource) {
        if (!(ressource instanceof Map<?, ?> donnees)) {
            return null;
        }

        Object id = donnees.get("id");
        if (id == null) {
            id = donnees.get("rowid");
        }

        if (id instanceof Number nombre) {
            return nombre.longValue();
        }

        if (id instanceof String texte && !texte.isBlank()) {
            return Long.parseLong(texte);
        }

        return null;
    }
}
