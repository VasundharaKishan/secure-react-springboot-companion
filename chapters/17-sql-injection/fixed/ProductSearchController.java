// FIXED — copy to:
//   backend/src/main/java/com/securitybook/app/product/ProductSearchController.java
//
// Two changes from the vulnerable version:
//   1. NamedParameterJdbcTemplate with a placeholder (`:search`) instead of
//      string concatenation.
//   2. Wildcards (`%`) live in the application, not in the user input.
package com.securitybook.app.product;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSearchController {

  private final NamedParameterJdbcTemplate namedJdbc;

  @GetMapping("/search")
  public List<ProductDto> search(@RequestParam String q) {

    // The driver sends the SQL and the parameter separately. Postgres parses
    // the SQL once, then binds `:search` as a value — there is no path by
    // which the value can be re-interpreted as SQL syntax.
    String sql = "SELECT id, name, price, image_url FROM products " +
                 "WHERE name ILIKE :search ORDER BY name LIMIT 50";

    return namedJdbc.query(
        sql,
        Map.of("search", "%" + sanitizeLikeWildcards(q) + "%"),
        productRowMapper
    );
  }

  // The user could still type `%` or `_` to manipulate the LIKE pattern
  // (annoying, not exploitable). Escape them so the input is treated as
  // literal text.
  private String sanitizeLikeWildcards(String input) {
    return input
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }

  private final RowMapper<ProductDto> productRowMapper = (rs, n) -> new ProductDto(
      rs.getString("id"),
      rs.getString("name"),
      rs.getBigDecimal("price"),
      rs.getString("image_url")
  );

  public record ProductDto(String id, String name, BigDecimal price, String imageUrl) {}
}
