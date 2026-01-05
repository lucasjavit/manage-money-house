package com.managehouse.money.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.File;
import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static files from /app/static with fallback to index.html for SPA
        registry.addResourceHandler("/**")
                .addResourceLocations("file:/app/static/", "classpath:/static/", "classpath:/public/")
                .setCachePeriod(0)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Se é uma requisição para API ou actuator, não processar
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
                            return null;
                        }

                        Resource requestedResource = location.createRelative(resourcePath);

                        // Se o recurso existe (arquivo real), retorna ele
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // Se não é um arquivo real, retornar index.html para SPA routing
                        File indexFile = new File("/app/static/index.html");
                        if (indexFile.exists()) {
                            try {
                                return location.createRelative("index.html");
                            } catch (IOException e) {
                                // Se falhar, tentar classpath
                                return new ClassPathResource("static/index.html");
                            }
                        }

                        return null;
                    }
                });
    }
}

