// FIXED — canonical path:
//   backend/src/main/java/com/securitybook/app/admin/AdminController.java
//
// Chapter 11 — admin endpoints carry @PreAuthorize("hasRole('ADMIN')").
// The vulnerable variant (chapters/11-authorization-fundamentals/vulnerable/)
// removes that annotation; the URL-level config in SecurityConfig only
// requires authentication, not a specific role, so any logged-in user
// reaches the endpoint.
package com.securitybook.app.admin;

import com.securitybook.app.user.User;
import com.securitybook.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

  private final UserRepository users;

  /**
   * Lists every user — including emails and roles. Sensitive enough that
   * it should be restricted to administrators.
   *
   * The @PreAuthorize annotation is the security boundary. Removing it
   * leaves the endpoint reachable by anyone with a valid JWT, which
   * defeats the entire authorization model.
   */
  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public List<Map<String, Object>> listAllUsers() {
    return users.findAll().stream()
        .map(u -> Map.<String, Object>of(
            "id", u.getId(),
            "email", u.getEmail(),
            "role", u.getRole()))
        .toList();
  }
}
