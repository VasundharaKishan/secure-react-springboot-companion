// FIXED — this is the canonical version that ships in:
//   backend/src/main/java/com/securitybook/app/security/JwtService.java
//
// Two defenses against the alg:none bug:
//   1. JJWT 0.12's parser().verifyWith(key) constructs a verifier locked
//      to the key type. A token with alg=none never reaches the payload.
//   2. requireIssuer pins the expected issuer claim, blocking tokens
//      forged by a different service that happens to share the secret.
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
      throw new IllegalStateException("JWT secret must be ≥32 bytes for HS256");
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

  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)        // ← rejects unsigned and wrong-alg tokens
        .requireIssuer(issuer)          // ← blocks cross-service token reuse
        .build()
        .parseSignedClaims(token)       // ← parseSigned* refuses alg=none
        .getPayload();
  }

  public UUID userIdFrom(Claims claims) {
    return UUID.fromString(claims.getSubject());
  }
}
