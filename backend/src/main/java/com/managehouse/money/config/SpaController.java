package com.managehouse.money.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    /**
     * Forward all non-API routes to index.html for SPA routing
     * This allows React Router to handle client-side routing
     */
    @GetMapping(value = {
        "/",
        "/{path:[^\\.]*}",
        "/{path:^(?!api|actuator).*}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
