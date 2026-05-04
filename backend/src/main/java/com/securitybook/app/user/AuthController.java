package com.securitybook.app.user;

import com.securitybook.app.security.JwtService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    return users.findByEmail(req.email())
        .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
        .map(u -> ResponseEntity.ok((Object) Map.of(
            "token", jwt.issue(u),
            "userId", u.getId(),
            "role", u.getRole())))
        .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
  }

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
}
