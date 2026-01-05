package com.managehouse.money.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Controller
public class SpaErrorController implements ErrorController {

    @RequestMapping("/error")
    public void handleError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        // Não processar requisições de API
        if (requestUri != null && (requestUri.startsWith("/api") || requestUri.startsWith("/actuator"))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
            return;
        }

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // Se for 404, servir o index.html para SPA routing
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                File indexFile = new File("/app/static/index.html");
                if (indexFile.exists() && indexFile.canRead()) {
                    response.setContentType("text/html");
                    response.setStatus(HttpStatus.OK.value());
                    Files.copy(indexFile.toPath(), response.getOutputStream());
                    return;
                }
            }
        }

        // Para outros erros, deixar o comportamento padrão
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
    }
}
