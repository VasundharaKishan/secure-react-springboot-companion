// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/user/User.java
//
// The bug is the addition of @ToString. Lombok generates a toString()
// that includes EVERY field — including passwordHash. The hash then
// flows into:
//   - log lines whenever someone writes log.info("user = {}", user)
//   - exception messages from Spring's binding/serialization stack
//   - Datadog/Splunk/Elastic indices, where it's now searchable
//   - support tickets, when an engineer pastes a stack trace
//
// At cost factor 12 the bcrypt hashes are still expensive to crack — but
// they're now exfiltrated outside your trust boundary, where attackers
// have unbounded compute.
package com.securitybook.app.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString          // ← BUG: includes passwordHash by default
public class User {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;          // ← appears in toString output

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 16)
  private Role role;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public enum Role { USER, SELLER, ADMIN }
}
