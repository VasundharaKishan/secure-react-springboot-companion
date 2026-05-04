package com.securitybook.app.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for the Chapter 17 SQL injection fix.
 *
 * Asserts the FIXED ProductSearchController returns zero rows for the
 * UNION-based payload that exfiltrated user data from the vulnerable version.
 *
 * Run against a real Postgres (Testcontainers) — H2 syntax differences would
 * mask the real-world behavior, exactly the trap Chapter 28 warns about.
 */
@SpringBootTest
@Testcontainers
class ProductSearchSqlInjectionTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withUsername("secbook")
          .withPassword("secbook_test")
          .withDatabaseName("secbook");

  @DynamicPropertySource
  static void dataSourceProps(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private WebApplicationContext ctx;

  private MockMvc mvc() {
    return MockMvcBuilders.webAppContextSetup(ctx).build();
  }

  @Test
  void unionPayloadDoesNotExtractUserHashes() throws Exception {
    String payload = "'  UNION  SELECT  id::text, email||':'||password_hash, '0'::numeric, NULL FROM users  --";

    mvc().perform(get("/api/products/search").param("q", payload))
        .andExpect(status().isOk())
        // No bcrypt hashes anywhere in the response body
        .andExpect(jsonPath("$").value(not(containsString("$2a$"))))
        .andExpect(jsonPath("$").value(not(containsString("$2b$"))))
        .andExpect(jsonPath("$").value(not(containsString("@"))));   // no email leak
  }

  @Test
  void benignSearchStillWorks() throws Exception {
    mvc().perform(get("/api/products/search").param("q", "keyboard"))
        .andExpect(status().isOk());
  }

  @Test
  void wildcardCharactersInUserInputAreEscaped() throws Exception {
    // A `%` in user input should be treated as a literal % search,
    // not a "match anything" wildcard.
    mvc().perform(get("/api/products/search").param("q", "%"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }
}
