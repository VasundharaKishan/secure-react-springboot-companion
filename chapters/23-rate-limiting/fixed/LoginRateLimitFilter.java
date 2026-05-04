// FIXED — same as the file shipped at:
//   backend/src/main/java/com/securitybook/app/security/LoginRateLimitFilter.java
package com.securitybook.app.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Order(1)
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final int  MAX_PER_MINUTE = 5;
  private static final long REFILL_SECONDS = 60;

  private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                  FilterChain chain) throws ServletException, IOException {

    if (!isLoginPost(req)) {
      chain.doFilter(req, res);
      return;
    }

    String ip = clientIp(req);
    Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());

    if (bucket.tryConsume(1)) {
      chain.doFilter(req, res);
    } else {
      res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      res.setHeader("Retry-After", String.valueOf(REFILL_SECONDS));
      res.setContentType("application/json");
      res.getWriter().write(
          "{\"error\":\"Too many login attempts. Try again in " + REFILL_SECONDS + " seconds.\"}");
    }
  }

  private boolean isLoginPost(HttpServletRequest req) {
    return "POST".equalsIgnoreCase(req.getMethod())
        && "/api/auth/login".equals(req.getRequestURI());
  }

  private String clientIp(HttpServletRequest req) {
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
    return req.getRemoteAddr();
  }

  private Bucket newBucket() {
    return Bucket.builder()
        .addLimit(limit -> limit
            .capacity(MAX_PER_MINUTE)
            .refillIntervally(MAX_PER_MINUTE, Duration.ofSeconds(REFILL_SECONDS)))
        .build();
  }
}
