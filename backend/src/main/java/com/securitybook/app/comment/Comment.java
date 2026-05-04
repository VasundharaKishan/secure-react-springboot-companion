package com.securitybook.app.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "author_email", nullable = false, length = 255)
  private String authorEmail;

  // Stored as-is — sanitization happens at write time in the FIXED controller,
  // and React escapes at render time. The vulnerable controller skips the
  // sanitization step.
  @Column(name = "body", nullable = false, length = 4000)
  private String body;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
