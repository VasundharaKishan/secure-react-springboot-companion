// FIXED — same as the file shipped at:
//   backend/src/main/java/com/securitybook/app/admin/AdminController.java
package com.securitybook.app.admin;

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

  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")        // ← the security boundary
  public List<Map<String, Object>> listAllUsers() {
    return users.findAll().stream()
        .map(u -> Map.<String, Object>of(
            "id", u.getId(),
            "email", u.getEmail(),
            "role", u.getRole()))
        .toList();
  }
}
