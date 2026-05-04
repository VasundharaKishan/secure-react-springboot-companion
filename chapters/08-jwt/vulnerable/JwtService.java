// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/security/JwtService.java
//
// The bug: parses tokens by trusting the `alg` header. JJWT 0.12 won't let
// us literally invoke `parse(token).getBody()` on an unsigned token, so the
// vulnerable variant emulates the historical "alg=none" library bug by
// reading the Base64-decoded claims directly from the token segments
// without checking the signature segment at all.
//
// Real-world examples: jwt-go pre-2017, jose4j pre-0.5.0, multiple Node
// libraries. Today's libraries reject alg=none by default — but every
// release where they did not is now a CVE.
package com.securitybook.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securitybook.app.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

  private final SecretKey signingKey;
  private final Duration ttl;
  private final String issuer;
  private final ObjectMapper json = new ObjectMapper();

  public JwtService(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.access-token-ttl}") Duration ttl,
                    @Value("${app.jwt.issuer}") String issuer) {
    this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
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
   * VULNERABLE: trusts whatever the client sends as long as the token has
   * three segments. Tokens with `alg: none` and an empty signature pass
   * straight through.
   */
  public Claims parse(String token) {
    try {
      String[] parts = token.split("\\.");
      String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
      Map<String, Object> header = json.readValue(headerJson, Map.class);

      // The "trust the alg header" bug. If the attacker says "none", we
      // skip signature verification entirely.
      String alg = (String) header.get("alg");
      if (!"none".equalsIgnoreCase(alg)) {
        // For HS256 we'd normally verify here. For brevity we still skip,
        // because chapter 8's point is what happens when we don't enforce
        // a signature. (The fixed version handles HS256 correctly.)
      }

      String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      Map<String, Object> claimsMap = json.readValue(payloadJson, Map.class);
      DefaultClaims claims = new DefaultClaims(claimsMap);
      return claims;
    } catch (Exception e) {
      throw new RuntimeException("Invalid token", e);
    }
  }

  public UUID userIdFrom(Claims claims) {
    return UUID.fromString(claims.getSubject());
  }
}
