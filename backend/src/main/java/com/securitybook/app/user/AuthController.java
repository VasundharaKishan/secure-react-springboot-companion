package com.securitybook.app.user;

import com.securitybook.app.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
    return users.findByEmail(req.email())
        .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
        .map(u -> ResponseEntity.ok((Object) Map.of(
            "token", jwt.issue(u),
            "userId", u.getId(),
            "role", u.getRole())))
        .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
  }

  /**
   * Minimal self-registration. Used by Chapter 13 / 16 attack scripts to
   * spin up throwaway victims without polluting the seed data.
   *
   * Production registration would add: email verification, captcha,
   * rate limiting, password breach check (HIBP), and probably a separate
   * onboarding flow per role.
   */
  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
    if (users.existsByEmail(req.email())) {
      return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
    }
    User.Role role;
    try {
      role = User.Role.valueOf(req.role());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
    }
    User u = users.save(User.builder()
        .id(UUID.randomUUID())
        .email(req.email())
        .passwordHash(encoder.encode(req.password()))
        .role(role)
        .createdAt(Instant.now())
        .build());
    return ResponseEntity.status(201).body(Map.of(
        "id", u.getId(),
        "email", u.getEmail(),
        "role", u.getRole()));
  }

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  public record RegisterRequest(
      @Email @NotBlank @Size(max = 255) String email,
      @NotBlank @Size(min = 8, max = 128) String password,
      @NotBlank String role
  ) {}
}
