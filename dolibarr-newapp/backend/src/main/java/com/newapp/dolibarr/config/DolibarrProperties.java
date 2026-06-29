package com.newapp.dolibarr.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dolibarr")
public class DolibarrProperties {

    private String baseUrl;
    private String apiKey;
    private List<Long> protectedUserIds = new ArrayList<>();
    private List<String> protectedUserLogins = new ArrayList<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<Long> getProtectedUserIds() {
        return protectedUserIds;
    }

    public void setProtectedUserIds(List<Long> protectedUserIds) {
        this.protectedUserIds = protectedUserIds;
    }

    public List<String> getProtectedUserLogins() {
        return protectedUserLogins;
    }

    public void setProtectedUserLogins(List<String> protectedUserLogins) {
        this.protectedUserLogins = protectedUserLogins;
    }
}
