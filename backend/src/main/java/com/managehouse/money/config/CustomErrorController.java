package com.managehouse.money.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<String> handleError() {
        // Tentar encontrar o index.html
        StringBuilder debug = new StringBuilder();
        debug.append("Debugando localização do index.html:\n\n");

        // Verificar /app/static/index.html
        File appStaticIndex = new File("/app/static/index.html");
        debug.append("1. /app/static/index.html exists: ").append(appStaticIndex.exists()).append("\n");
        if (appStaticIndex.exists()) {
            debug.append("   readable: ").append(appStaticIndex.canRead()).append("\n");
            debug.append("   size: ").append(appStaticIndex.length()).append(" bytes\n");
        }

        // Verificar classpath:/static/index.html
        Resource classpathResource = new ClassPathResource("static/index.html");
        debug.append("\n2. classpath:/static/index.html exists: ").append(classpathResource.exists()).append("\n");

        // Verificar classpath:/public/index.html
        Resource publicResource = new ClassPathResource("public/index.html");
        debug.append("\n3. classpath:/public/index.html exists: ").append(publicResource.exists()).append("\n");

        // Listar conteúdo de /app/static
        debug.append("\n4. Conteúdo de /app/static:\n");
        try {
            File staticDir = new File("/app/static");
            if (staticDir.exists() && staticDir.isDirectory()) {
                File[] files = staticDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        debug.append("   - ").append(f.getName())
                             .append(" (").append(f.isDirectory() ? "dir" : "file")
                             .append(", ").append(f.length()).append(" bytes)\n");
                    }
                } else {
                    debug.append("   (vazio)\n");
                }
            } else {
                debug.append("   (diretório não existe)\n");
            }
        } catch (Exception e) {
            debug.append("   Erro ao listar: ").append(e.getMessage()).append("\n");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(debug.toString());
    }
}
