package com.managehouse.money.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import java.io.File;

@Slf4j
@Controller
public class SpaErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Resource> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        log.debug("Error handler called for URI: {}, status: {}", requestUri, status);

        // Não processar requisições de API ou actuator
        if (requestUri != null && (requestUri.startsWith("/api") || requestUri.startsWith("/actuator"))) {
            return ResponseEntity.notFound().build();
        }

        // Não processar requisições de assets estáticos
        if (requestUri != null && (requestUri.contains(".") &&
            (requestUri.endsWith(".js") || requestUri.endsWith(".css") ||
             requestUri.endsWith(".png") || requestUri.endsWith(".jpg") ||
             requestUri.endsWith(".ico") || requestUri.endsWith(".svg") ||
             requestUri.endsWith(".woff") || requestUri.endsWith(".woff2")))) {
            return ResponseEntity.notFound().build();
        }

        // Para rotas SPA (404), servir index.html
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                // Tentar primeiro o arquivo no sistema de arquivos (produção)
                File indexFile = new File("/app/static/index.html");
                if (indexFile.exists() && indexFile.canRead()) {
                    log.debug("Serving index.html from file system for SPA route: {}", requestUri);
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(new FileSystemResource(indexFile));
                }

                // Fallback para classpath (desenvolvimento)
                Resource classpathIndex = new ClassPathResource("static/index.html");
                if (classpathIndex.exists()) {
                    log.debug("Serving index.html from classpath for SPA route: {}", requestUri);
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(classpathIndex);
                }

                log.warn("index.html not found for SPA route: {}", requestUri);
            }
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
