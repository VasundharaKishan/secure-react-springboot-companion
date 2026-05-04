// FIXED — same as the file shipped at:
//   backend/src/main/java/com/securitybook/app/order/OrderController.java
//
// Adds the ownership check that the vulnerable version omits.
package com.securitybook.app.order;

import com.securitybook.app.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class OrderController {

  private final OrderRepository orders;

  @GetMapping("/{userId}/orders")
  public ResponseEntity<?> listOrders(@PathVariable UUID userId,
                                      @AuthenticationPrincipal User principal) {

    if (principal == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
    }

    // The fix: caller may only see their own orders, unless they are admin.
    boolean isOwner = principal.getId().equals(userId);
    boolean isAdmin = principal.getRole() == User.Role.ADMIN;
    if (!isOwner && !isAdmin) {
      return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }

    return ResponseEntity.ok(orders.findByUserId(userId));
  }
}
