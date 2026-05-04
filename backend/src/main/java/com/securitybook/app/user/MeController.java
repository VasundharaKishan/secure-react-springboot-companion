package com.securitybook.app.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Returns the authenticated user. Used by Chapter 8's attack.sh to detect
 * whether a forged JWT was accepted.
 */
@RestController
@RequestMapping("/api")
public class MeController {

  @GetMapping("/me")
  public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
    if (user == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
    }
    return ResponseEntity.ok(Map.of(
        "id", user.getId(),
        "email", user.getEmail(),
        "role", user.getRole()
    ));
  }
}
