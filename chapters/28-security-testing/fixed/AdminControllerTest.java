// FIXED — copy to:
//   backend/src/test/java/com/securitybook/app/admin/AdminControllerTest.java
//
// Three tests, not one. The negative cases are the security tests:
//   1. Admin sees 200 (functional happy path)
//   2. Non-admin sees 403 (the actual security boundary)
//   3. Anonymous sees 401 (authentication boundary)
//
// All three must hold for the controller to be considered safe. A future
// regression that removes @PreAuthorize fails test #2 immediately, before
// the change ever reaches code review.
package com.securitybook.app.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AdminControllerTest {

  @Autowired private WebApplicationContext ctx;
  private MockMvc mvc() { return MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build(); }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanListUsers() throws Exception {
    mvc().perform(get("/api/admin/users")).andExpect(status().isOk());
  }

  // The negative case. This is the real security test.
  @Test
  @WithMockUser(roles = "USER")
  void nonAdminGetsForbidden() throws Exception {
    mvc().perform(get("/api/admin/users")).andExpect(status().isForbidden());
  }

  @Test
  @WithAnonymousUser
  void anonymousGetsUnauthorized() throws Exception {
    mvc().perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
  }
}
