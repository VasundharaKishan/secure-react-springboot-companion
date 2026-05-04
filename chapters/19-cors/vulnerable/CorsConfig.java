// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/config/CorsConfig.java
//
// You also need to remove the @Bean CorsConfigurationSource from
// SecurityConfig.java (or this bean will be ambiguous). The chapter
// README walks through the swap step by step.
//
// The bug: allowedOriginPatterns("*") combined with allowCredentials(true)
// reflects ANY origin's credentials back. Spring 5.3+ blocks the literal
// allowedOrigins("*") + credentials combo with an exception, so attackers
// (and unwary developers) reach for `allowedOriginPatterns("*")` —
// which has no such guard.
//
// Result: an attacker page on https://evil.example sends:
//   fetch('https://api.example.com/api/me', { credentials: 'include' })
// and reads the response, including the victim's session.
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
    // BUG: pattern wildcard with credentials. Reflects ANY origin.
    cfg.setAllowedOriginPatterns(List.of("*"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);                 // ← without this it would be merely lax, not exploitable
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/api/**", cfg);
    return src;
  }
}
