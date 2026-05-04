// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/order/OrderController.java
//
// The bug: any authenticated user can request /api/users/<any-other-user>/orders
// and get back their order history. The path parameter is trusted.
//
// This is OWASP API Top 10 #1: BOLA. Found in roughly half of public bug
// bounty reports.
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

    // BUG: no ownership check. The principal is authenticated but their identity
    // is never compared to {userId}. A logged-in user can fetch any other user's
    // order history by changing the path parameter.
    List<Order> result = orders.findByUserId(userId);
    return ResponseEntity.ok(result);
  }
}
