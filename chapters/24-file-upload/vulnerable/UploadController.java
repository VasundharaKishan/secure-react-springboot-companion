// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/upload/UploadController.java
//
// Two defects:
//   1. Trusts the file extension to decide whether the upload is an image.
//      An attacker can rename evil.html to evil.png and the check passes.
//   2. Serves the file back with a Content-Type derived from extension.
//      Combined with no `nosniff`, browsers infer text/html from content
//      and execute any embedded <script>.
package com.securitybook.app.upload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
@Slf4j
public class UploadController {

  private static final Path UPLOAD_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "secbook-uploads");

  static {
    try { Files.createDirectories(UPLOAD_DIR); }
    catch (IOException e) { throw new RuntimeException(e); }
  }

  @PostMapping
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
    String name = file.getOriginalFilename();
    if (name == null) return ResponseEntity.badRequest().body(Map.of("error", "missing name"));

    // BUG: extension check only. evil.html.png passes.
    String lower = name.toLowerCase();
    if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".gif")) {
      return ResponseEntity.status(415).body(Map.of("error", "Only image extensions accepted"));
    }

    String id = UUID.randomUUID() + "-" + name;   // BUG: name preserved unsanitized
    Path target = UPLOAD_DIR.resolve(id);
    Files.write(target, file.getBytes());
    return ResponseEntity.ok(Map.of("id", id, "size", file.getSize()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Resource> download(@PathVariable String id) {
    Path target = UPLOAD_DIR.resolve(id);   // BUG: no path-traversal guard
    if (!Files.exists(target)) return ResponseEntity.notFound().build();

    // BUG: no Content-Type override, no X-Content-Type-Options. Tomcat picks
    // the type from extension, browser sniffs the content if extension is
    // ambiguous, and HTML inside the file executes.
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + id + "\"")
        .body(new FileSystemResource(target));
  }
}
