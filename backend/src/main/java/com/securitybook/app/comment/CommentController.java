// FIXED — canonical path:
//   backend/src/main/java/com/securitybook/app/comment/CommentController.java
//
// Server-side defense: sanitize HTML on write (Jsoup-style allowlist of safe
// tags only). The frontend ALSO does the right thing by avoiding
// dangerouslySetInnerHTML — see chapters/15-xss/fixed/CommentList.tsx.
//
// Defense in depth: never rely on client-only sanitization.
package com.securitybook.app.comment;

import jakarta.validation.constraints.NotBlank;
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
  public Comment create(@RequestBody CreateRequest req) {
    String safeBody = sanitizeHtml(req.body());
    return comments.save(Comment.builder()
        .id(UUID.randomUUID())
        .productId(req.productId())
        .authorEmail(req.authorEmail())
        .body(safeBody)
        .createdAt(Instant.now())
        .build());
  }

  /**
   * Minimal allow-list sanitizer. In a real codebase use OWASP Java HTML
   * Sanitizer or Jsoup. The point of this method is to show the position
   * the sanitizer occupies in the request lifecycle — at the trust boundary,
   * before the value is stored.
   */
  private String sanitizeHtml(String input) {
    if (input == null) return "";
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  public record CreateRequest(@NotBlank UUID productId,
                              @NotBlank String authorEmail,
                              @NotBlank @Size(max = 4000) String body) {}
}
