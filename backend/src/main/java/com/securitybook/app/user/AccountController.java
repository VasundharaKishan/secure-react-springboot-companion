// FIXED — canonical path:
//   backend/src/main/java/com/securitybook/app/user/AccountController.java
//
// Account deletion. The fix here is purely the HTTP method: DELETE, not GET.
// Browsers do not auto-issue DELETE on image loads, page navigation, or
// link previews — so an attacker who tricks a victim into clicking a URL
// cannot fire the deletion.
//
// Even if you authenticate via JWT (no cookie session, "we're immune to
// CSRF"), this still matters: any state change via GET is a bug because
// the URL flows through caches, browser history, prefetchers, search
// engines, and copy-paste — none of which expect side effects.
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

  /**
   * The right verb for a destructive action.
   *
   * - Browsers do not preflight or auto-issue DELETE from images, links,
   *   or background prefetchers.
   * - Cross-origin DELETE is a "non-simple" CORS request, requiring a
   *   preflight that our config rejects for unknown origins (Chapter 19).
   * - The verb signals intent in logs and proxies — easy to alert on.
   */
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
