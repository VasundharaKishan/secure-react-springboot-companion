// FIXED — same as the file shipped at:
//   backend/src/main/java/com/securitybook/app/user/User.java
//
// Two protections:
//   1. NO @ToString annotation. The default Object.toString() is fine —
//      it returns "User@hashcode" with no field values.
//   2. If you DO want a toString (often handy for debugging), use
//      @ToString(exclude = {"passwordHash"}) so the field is explicitly
//      skipped.
//
// The wider principle: every entity with sensitive fields should declare
// a serialization-time exclusion list (Jackson @JsonIgnore, Lombok exclude,
// custom DTOs). It is much easier to enforce "always opt out of the field"
// than to remember "never log this object."
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
// no @ToString — default Object.toString prints class@hashcode only.
public class User {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  @ToString.Exclude               // belt + braces if a future @ToString is added
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 16)
  private Role role;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public enum Role { USER, SELLER, ADMIN }
}
