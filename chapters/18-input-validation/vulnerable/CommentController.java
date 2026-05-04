// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/comment/CommentController.java
//
// Two missing defenses:
//   1. No @Valid on the @RequestBody — Bean Validation annotations on
//      the DTO are simply ignored. Even if the record had constraints,
//      they wouldn't fire.
//   2. The DTO's body field has no @Size limit — attacker posts a 50 MB
//      string. Spring buffers it in memory; under load this OOM-kills
//      the JVM.
//
// Note this version also drops the sanitizeHtml call — see Chapter 15 for
// the XSS impact. Each chapter focuses on one defect; the production-grade
// version (chapters/18-input-validation/fixed/) shows both fixes together.
package com.securitybook.app.comment;

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
  public Comment create(@RequestBody CreateRequest req) {     // BUG: no @Valid
    return comments.save(Comment.builder()
        .id(UUID.randomUUID())
        .productId(req.productId())
        .authorEmail(req.authorEmail())
        .body(req.body())
        .createdAt(Instant.now())
        .build());
  }

  // BUG: no @Size constraint on body. A POST with a 50 MB body buffers
  // the entire request, then writes 50 MB to the database. Repeat in a
  // loop for instant memory pressure.
  public record CreateRequest(UUID productId, String authorEmail, String body) {}
}
