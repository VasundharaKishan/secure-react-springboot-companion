// VULNERABLE — copy to:
//   backend/src/test/java/com/securitybook/app/admin/AdminControllerTest.java
//
// This test PASSES against the vulnerable AdminController (no @PreAuthorize)
// — and that's the bug. The test author wrote @WithMockUser(roles="ADMIN")
// to authenticate the request, then asserted 200, then went home. The
// test never asks "would this also pass for a USER?" — and as long as
// the controller doesn't enforce a role, it does.
//
// Hundreds of bugs ship every year because nobody writes the negative
// case. The "happy path with mock admin" test gives green CI, false
// confidence, and zero protection.
package com.securitybook.app.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

  // The only test. Passes regardless of whether the controller actually
  // enforces the ADMIN role.
  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanListUsers() throws Exception {
    mvc().perform(get("/api/admin/users")).andExpect(status().isOk());
  }
}
