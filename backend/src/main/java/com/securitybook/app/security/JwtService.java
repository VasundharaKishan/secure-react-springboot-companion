package com.securitybook.app.security;

import com.securitybook.app.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates HS256 JWTs using JJWT 0.12.x.
 *
 * Chapter 8 swaps this implementation for the {@code vulnerable/} variant
 * that accepts unsigned tokens (`alg: none`) — see the chapter README.
 */
@Service
public class JwtService {

  private final SecretKey signingKey;
  private final Duration ttl;
  private final String issuer;

  public JwtService(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.access-token-ttl}") Duration ttl,
                    @Value("${app.jwt.issuer}") String issuer) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException(
          "JWT secret must be at least 32 bytes for HS256 — see Chapter 26");
    }
    this.signingKey = new SecretKeySpec(bytes, "HmacSHA256");
    this.ttl = ttl;
    this.issuer = issuer;
  }

  public String issue(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole().name())
        .issuer(issuer)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(ttl)))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Validates a token and returns the parsed claims.
   *
   * Throws JwtException on any failure (bad signature, expiry, malformed).
   * Callers should treat the throw as "request unauthenticated."
   */
  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .requireIssuer(issuer)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public UUID userIdFrom(Claims claims) {
    return UUID.fromString(claims.getSubject());
  }
}
