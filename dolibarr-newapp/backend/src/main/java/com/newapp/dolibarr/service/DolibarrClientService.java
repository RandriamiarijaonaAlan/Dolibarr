package com.newapp.dolibarr.service;

import com.newapp.dolibarr.config.DolibarrProperties;
import com.newapp.dolibarr.dto.DolibarrConnectionResponse;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DolibarrClientService {

    private final DolibarrProperties dolibarrProperties;
    private final RestTemplate restTemplate;

    public DolibarrClientService(DolibarrProperties dolibarrProperties, RestTemplateBuilder restTemplateBuilder) {
        this.dolibarrProperties = dolibarrProperties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public DolibarrConnectionResponse testConnection() {
        String baseUrl = dolibarrProperties.getBaseUrl();
        String testUrl = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/users/info")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("DOLAPIKEY", dolibarrProperties.getApiKey());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    testUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

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
}
