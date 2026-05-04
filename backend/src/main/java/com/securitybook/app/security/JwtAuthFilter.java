package com.securitybook.app.security;

import com.securitybook.app.user.User;
import com.securitybook.app.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads `Authorization: Bearer <jwt>` and populates the SecurityContext.
 *
 * Failures are silent — the filter chain falls through to the next handler,
 * which results in 401 if the endpoint requires authentication. The point
 * of a missing or bad token is "no identity," not "stop the request."
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwt;
  private final UserRepository users;

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                  FilterChain chain) throws ServletException, IOException {

    String header = req.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        Claims claims = jwt.parse(token);
        UUID userId = jwt.userIdFrom(claims);
        User user = users.findById(userId).orElse(null);
        if (user != null) {
          var auth = new UsernamePasswordAuthenticationToken(
              user, null,
              List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
          auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (JwtException ignored) {
        // Bad token — leave SecurityContext empty; protected endpoints will 401.
      }
    }
    chain.doFilter(req, res);
  }
}
