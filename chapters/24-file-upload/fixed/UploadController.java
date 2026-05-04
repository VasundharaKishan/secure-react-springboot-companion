// FIXED — same as the file shipped at:
//   backend/src/main/java/com/securitybook/app/upload/UploadController.java
package com.securitybook.app.upload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
      return ResponseEntity.badRequest().body(Map.of("error", "File missing or too large"));
    }

    byte[] head = new byte[8];
    int read = file.getInputStream().read(head);
    if (read < 4 || !isAllowedImage(head)) {
      return ResponseEntity.status(415).body(Map.of("error", "Only PNG, JPEG, GIF accepted"));
    }

    String safeId = UUID.randomUUID().toString();
    Path target = UPLOAD_DIR.resolve(safeId);
    Files.write(target, file.getBytes());

    return ResponseEntity.ok(Map.of("id", safeId, "size", file.getSize()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Resource> download(@PathVariable String id) {
    String safe = id.replaceAll("[^a-zA-Z0-9-]", "");
    Path target = UPLOAD_DIR.resolve(safe);
    if (!Files.exists(target)) return ResponseEntity.notFound().build();

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safe + ".png\"")
        .header("X-Content-Type-Options", "nosniff")
        .body(new FileSystemResource(target));
  }

  private boolean isAllowedImage(byte[] head) {
    if (head[0] == (byte) 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47) return true;
    if (head[0] == (byte) 0xFF && head[1] == (byte) 0xD8 && head[2] == (byte) 0xFF) return true;
    if (head[0] == 0x47 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x38) return true;
    return false;
  }
}
