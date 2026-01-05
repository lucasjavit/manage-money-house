package com.managehouse.money.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve frontend static files
        registry.addResourceHandler("/**")
                .addResourceLocations("file:/app/static/", "classpath:/static/", "classpath:/public/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // Se o recurso existe (arquivos estáticos como .js, .css, .png), retorna ele
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // Para rotas SPA (que não são arquivos), retorna index.html
                        // Mas não para rotas de API
                        if (!resourcePath.startsWith("api/")) {
                            Resource indexHtml = new ClassPathResource("static/index.html");
                            if (!indexHtml.exists()) {
                                indexHtml = location.createRelative("index.html");
                            }
                            return indexHtml;
                        }

                        return null;
                    }
                });
    }
}

