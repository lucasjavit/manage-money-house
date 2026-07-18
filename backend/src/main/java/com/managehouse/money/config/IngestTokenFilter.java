package com.managehouse.money.config;

import com.managehouse.money.service.ConfigurationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege a rota /api/ingest com um token compartilhado (header X-Ingest-Token),
 * comparado ao valor da config "ingest.token". O resto da API segue como está.
 * Registrado apenas em /api/ingest/* pelo IngestSecurityConfig.
 */
@RequiredArgsConstructor
public class IngestTokenFilter extends OncePerRequestFilter {

    private final ConfigurationService configurationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Preflight CORS não carrega o header; deixa passar.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String expected = configurationService.getIngestToken();
        String provided = request.getHeader("X-Ingest-Token");

        if (expected == null || expected.isBlank() || !constantTimeEquals(expected, provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid or missing X-Ingest-Token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
