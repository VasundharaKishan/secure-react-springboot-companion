// FIXED — canonical path:
//   backend/src/main/java/com/securitybook/app/product/ProductEditController.java
//
// Sellers may edit only their own products. The fix is the
// `seller.equals(product.getSellerId())` ownership check before the
// price update.
//
// Chapter 13 demonstrates that authentication + role check are not enough —
// you must also verify that the authenticated principal owns the specific
// resource being modified. Without this check, any user with role SELLER
// edits any other seller's products (a real Etsy-class bug).
package com.securitybook.app.product;

import com.securitybook.app.user.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductEditController {

  private final ProductRepository products;

  @PatchMapping("/{productId}/price")
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
  public ResponseEntity<?> updatePrice(@PathVariable UUID productId,
                                       @RequestBody PriceUpdate body,
                                       @AuthenticationPrincipal User caller) {

    Product product = products.findById(productId).orElse(null);
    if (product == null) return ResponseEntity.notFound().build();

    boolean isOwner = caller.getId().equals(product.getSellerId());
    boolean isAdmin = caller.getRole() == User.Role.ADMIN;
    if (!isOwner && !isAdmin) {
      // Two key behaviours: 403 (not 404), and a generic message —
      // never reveal whether the resource exists when authorization fails.
      return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }

    product.setPrice(body.price());
    products.save(product);
    return ResponseEntity.ok(product);
  }

  public record PriceUpdate(@NotNull @Positive BigDecimal price) {}
}
