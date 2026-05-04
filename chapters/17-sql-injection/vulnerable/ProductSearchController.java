// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/product/ProductSearchController.java
//
// This is the kind of code a developer writes the first time they need
// "search products by name." The bug: query string concatenated into SQL.
package com.securitybook.app.product;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSearchController {

  private final JdbcTemplate jdbcTemplate;

  @GetMapping("/search")
  public List<ProductDto> search(@RequestParam String q) {

    // BUG: user input concatenated directly into SQL.
    // An attacker who controls `q` controls the SQL parser.
    String sql = "SELECT id, name, price, image_url FROM products " +
                 "WHERE name LIKE '%" + q + "%' ORDER BY name LIMIT 50";

    return jdbcTemplate.query(sql, productRowMapper);
  }

  private final RowMapper<ProductDto> productRowMapper = (rs, n) -> new ProductDto(
      rs.getString("id"),
      rs.getString("name"),
      rs.getBigDecimal("price"),
      rs.getString("image_url")
  );

  public record ProductDto(String id, String name, BigDecimal price, String imageUrl) {}
}
