// FIXED — same as the file shipped at:
//   backend/src/main/java/com/securitybook/app/user/AccountController.java
package com.securitybook.app.user;

import com.securitybook.app.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

  private final UserRepository users;

  @DeleteMapping("/me")
  public ResponseEntity<?> deleteMe(@AuthenticationPrincipal User caller,
                                    HttpServletRequest req) {
    if (caller == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
    }
    users.deleteById(caller.getId());
    return ResponseEntity.ok(Map.of("deleted", caller.getId()));
  }
}
