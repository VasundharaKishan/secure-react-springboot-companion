// FIXED — overrides the canonical bean in:
//   backend/src/main/java/com/securitybook/app/config/SecurityConfig.java
//
// Two defenses:
//   1. Explicit allowlist of origins. No patterns. No wildcard.
//   2. Methods and headers also explicit, not "*".
//
// Adding new frontends to production means a deliberate config change —
// which is exactly the friction you want.
package com.securitybook.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(                  // exact origins, not patterns
        "http://localhost:5173",
        "https://app.example.com"
    ));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/api/**", cfg);
    return src;
  }
}
