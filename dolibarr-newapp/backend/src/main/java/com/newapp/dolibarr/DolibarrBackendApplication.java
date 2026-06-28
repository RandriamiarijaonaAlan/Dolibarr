package com.newapp.dolibarr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DolibarrBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DolibarrBackendApplication.class, args);
    }
}
