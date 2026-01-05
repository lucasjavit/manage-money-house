package com.managehouse.money.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
public class DebugController {

    @GetMapping("/debug/static")
    public ResponseEntity<String> debugStatic() {
        StringBuilder sb = new StringBuilder();
        sb.append("Debug Info:\n\n");

        File staticDir = new File("/app/static");
        sb.append("1. /app/static exists: ").append(staticDir.exists()).append("\n");
        sb.append("2. /app/static is directory: ").append(staticDir.isDirectory()).append("\n");

        if (staticDir.exists() && staticDir.isDirectory()) {
            sb.append("3. Files in /app/static:\n");
            File[] files = staticDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    sb.append("   - ").append(f.getName())
                      .append(" (").append(f.isDirectory() ? "dir" : "file")
                      .append(", ").append(f.length()).append(" bytes)\n");

                    if (f.isDirectory() && f.getName().equals("assets")) {
                        File[] assetsFiles = f.listFiles();
                        if (assetsFiles != null) {
                            for (File af : assetsFiles) {
                                sb.append("      - ").append(af.getName())
                                  .append(" (").append(af.length()).append(" bytes)\n");
                            }
                        }
                    }
                }
            }
        }

        File indexFile = new File("/app/static/index.html");
        sb.append("\n4. /app/static/index.html:\n");
        sb.append("   exists: ").append(indexFile.exists()).append("\n");
        sb.append("   readable: ").append(indexFile.canRead()).append("\n");
        sb.append("   size: ").append(indexFile.length()).append(" bytes\n");

        return ResponseEntity.ok(sb.toString());
    }
}
