// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/user/AccountController.java
//
// The bug: a destructive action exposed on GET. Two consequences:
//   1. Anything that auto-loads URLs (image tags, link previews, browser
//      prefetch, search-engine crawlers) can fire the deletion.
//   2. Cross-origin GETs are "simple requests" — no CORS preflight, no
//      Origin header enforcement on the server side.
//
// Even with a JWT in the Authorization header (and no cookies = "immune
// to CSRF"), this is still wrong, because:
//   - Some frontends store the token in localStorage and the React app
//     attaches it to every fetch including ones the user didn't intend
//   - Browser dev tools, browser history, and HTTP referer logs all
//     capture GET URLs verbatim. An attacker reading those gets
//     unrestricted action triggers.
package com.securitybook.app.user;

import com.securitybook.app.security.JwtService;
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

  // BUG: GET /api/account/delete deletes the account. Visiting the URL
  // is enough — no body, no method check, no CSRF token.
  @GetMapping("/delete")
  public ResponseEntity<?> deleteOnGet(@AuthenticationPrincipal User caller) {
    if (caller == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
    }
    users.deleteById(caller.getId());
    return ResponseEntity.ok(Map.of("deleted", caller.getId()));
  }
}
