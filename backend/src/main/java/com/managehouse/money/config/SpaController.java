package com.managehouse.money.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class SpaController {

    /**
     * Serve index.html for all non-API routes for SPA routing
     * This allows React Router to handle client-side routing
     */
    @GetMapping(value = {
        "/",
        "/{path:[^\\.]*}",
        "/{path:^(?!api|actuator).*}/**"
    })
    public void serveIndex(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setStatus(HttpStatus.OK.value());

        // Tentar ler de /app/static/index.html primeiro
        File indexFile = new File("/app/static/index.html");
        if (indexFile.exists() && indexFile.canRead()) {
            try (InputStream is = new FileInputStream(indexFile)) {
                StreamUtils.copy(is, response.getOutputStream());
            }
            return;
        }

        // Fallback para classpath
        Resource resource = new ClassPathResource("static/index.html");
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                StreamUtils.copy(is, response.getOutputStream());
            }
            return;
        }

        // Se não encontrou, retornar erro
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.getWriter().write("index.html not found");
    }
}
