package uk.ac.ed.acp.cw2.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;

import java.net.URL;

@Configuration
@EnableScheduling
public class IlpRestServiceConfig {

    @Bean
    public String getEndpointIlp(){
        String developerModeUrl = "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/";
        return System.getenv("ILP_ENDPOINT") != null ? System.getenv("ILP_ENDPOINT"): developerModeUrl;
    }

}
