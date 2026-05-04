// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/admin/AdminController.java
//
// The bug: no @PreAuthorize. The URL filter chain in SecurityConfig requires
// authentication for /api/** but does NOT require the ADMIN role — that's
// what method-level security exists for. Without the annotation any
// logged-in user (including alice with role USER) can list every user.
//
// This is the most common authorization bug in Spring Boot: developers add
// the @RestController, configure URL-level filters once, and forget that
// "authenticated" doesn't mean "authorized."
package com.securitybook.app.admin;

import com.securitybook.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
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

  // BUG: no @PreAuthorize. Any authenticated user reaches this method.
  @GetMapping("/users")
  public List<Map<String, Object>> listAllUsers() {
    return users.findAll().stream()
        .map(u -> Map.<String, Object>of(
            "id", u.getId(),
            "email", u.getEmail(),
            "role", u.getRole()))
        .toList();
  }
}
