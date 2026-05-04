// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/product/ProductEditController.java
//
// The bug: only the role is checked, not ownership. Any account with the
// SELLER role can edit any product's price — including a competitor's.
// A malicious seller drops a competitor's product to $0.01, the
// competitor sells out at a loss, brand reputation craters.
//
// This is OWASP API #1 (BOLA) again, on a different verb. Chapter 22
// covered the read variant; this one covers the write variant.
package com.securitybook.app.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductEditController {

  private final ProductRepository products;

  @PatchMapping("/{productId}/price")
  @PreAuthorize("hasAnyRole('SELLER','ADMIN')")        // role check only
  public ResponseEntity<?> updatePrice(@PathVariable UUID productId,
                                       @RequestBody PriceUpdate body) {

    // BUG: no ownership check. Any seller edits any product.
    Product product = products.findById(productId).orElse(null);
    if (product == null) return ResponseEntity.notFound().build();

    product.setPrice(body.price());
    products.save(product);
    return ResponseEntity.ok(product);
  }

  public record PriceUpdate(@NotNull @Positive BigDecimal price) {}
}
