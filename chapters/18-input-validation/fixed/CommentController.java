// FIXED — same as the file shipped at:
//   backend/src/main/java/com/securitybook/app/comment/CommentController.java
//
// Three defenses:
//   1. @Valid triggers Bean Validation on the request body
//   2. @Size(max=4000) caps body length at 4 KB
//   3. (separate from this chapter) sanitizeHtml escapes HTML before storage
//
// Defense in depth: validation also enforced at the database column
// length (length=4000 on the @Column). If validation is ever bypassed
// the persistence layer rejects.
package com.securitybook.app.comment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentRepository comments;

  @GetMapping
  public List<Comment> list(@RequestParam UUID productId) {
    return comments.findByProductIdOrderByCreatedAtDesc(productId);
  }

  @PostMapping
  public Comment create(@Valid @RequestBody CreateRequest req) {
    return comments.save(Comment.builder()
        .id(UUID.randomUUID())
        .productId(req.productId())
        .authorEmail(req.authorEmail())
        .body(sanitizeHtml(req.body()))
        .createdAt(Instant.now())
        .build());
  }

  private String sanitizeHtml(String input) {
    if (input == null) return "";
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  public record CreateRequest(
      @NotNull UUID productId,
      @NotBlank @Email @Size(max = 255) String authorEmail,
      @NotBlank @Size(max = 4000) String body
  ) {}
}
