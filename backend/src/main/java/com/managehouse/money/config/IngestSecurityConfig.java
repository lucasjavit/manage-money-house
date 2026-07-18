package com.managehouse.money.config;

import com.managehouse.money.service.ConfigurationService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra o IngestTokenFilter apenas na rota /api/ingest/*.
 * Não há Spring Security no projeto; este é o único ponto autenticado.
 */
@Configuration
public class IngestSecurityConfig {

    @Bean
    public FilterRegistrationBean<IngestTokenFilter> ingestTokenFilter(ConfigurationService configurationService) {
        FilterRegistrationBean<IngestTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IngestTokenFilter(configurationService));
        registration.addUrlPatterns("/api/ingest/*", "/api/ingest");
        registration.setName("ingestTokenFilter");
        return registration;
    }
}
