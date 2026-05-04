// FIXED — same as the file shipped at:
//   backend/src/main/java/com/securitybook/app/comment/CommentController.java
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
