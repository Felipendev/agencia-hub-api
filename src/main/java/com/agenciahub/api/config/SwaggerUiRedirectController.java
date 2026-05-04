package com.agenciahub.api.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Quick entry point: {@code GET /docs} → Swagger UI (same servlet context as {@code /api/v1}).
 */
@Controller
public class SwaggerUiRedirectController {

    @GetMapping("/docs")
    public String redirectToSwaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
}
