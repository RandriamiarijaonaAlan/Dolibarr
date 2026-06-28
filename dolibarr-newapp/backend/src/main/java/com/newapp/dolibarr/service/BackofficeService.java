package com.newapp.dolibarr.service;

import com.newapp.dolibarr.config.BackofficeProperties;
import org.springframework.stereotype.Service;

@Service
public class BackofficeService {

    private final BackofficeProperties backofficeProperties;

    public BackofficeService(BackofficeProperties backofficeProperties) {
        this.backofficeProperties = backofficeProperties;
    }

    public boolean verifierCode(String code) {
        return code != null && code.equals(backofficeProperties.getCodeUnique());
    }
}
