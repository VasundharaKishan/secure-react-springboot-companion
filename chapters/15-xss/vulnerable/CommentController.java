// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/comment/CommentController.java
//
// Stores comment bodies verbatim. When the frontend then renders them via
// dangerouslySetInnerHTML (see vulnerable/CommentList.tsx), any <script>
// tag in the comment runs in the victim's browser.
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
    // BUG: stored as-is. No HTML escaping. No allowlist. Whatever the
    // attacker sends will live in the database and ship to every browser
    // that views this product.
    return comments.save(Comment.builder()
        .id(UUID.randomUUID())
        .productId(req.productId())
        .authorEmail(req.authorEmail())
        .body(req.body())
        .createdAt(Instant.now())
        .build());
  }

  public record CreateRequest(@NotBlank UUID productId,
                              @NotBlank String authorEmail,
                              @NotBlank @Size(max = 4000) String body) {}
}
