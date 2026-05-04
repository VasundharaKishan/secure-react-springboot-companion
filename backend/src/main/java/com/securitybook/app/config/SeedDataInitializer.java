package com.securitybook.app.config;

import com.securitybook.app.product.Product;
import com.securitybook.app.product.ProductRepository;
import com.securitybook.app.user.User;
import com.securitybook.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Seeds users and products on first boot so chapter examples work
 * out of the box.
 *
 * Default credentials (development only):
 *   alice@example.com  / alice123!
 *   bob@example.com    / bob123!
 *   admin@example.com  / admin123!
 *   seller@example.com / seller123!
 *
 * Bcrypt hashes are computed at startup using the configured PasswordEncoder
 * (cost factor 12) — see Chapter 7 for why hardcoding hashes in SQL files is
 * a maintenance trap.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeedDataInitializer implements CommandLineRunner {

  private final UserRepository users;
  private final ProductRepository products;
  private final PasswordEncoder encoder;

  @Override
  public void run(String... args) {
    if (users.count() > 0) {
      log.info("Database already seeded — skipping");
      return;
    }

    UUID aliceId  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID bobId    = UUID.fromString("22222222-2222-2222-2222-222222222222");
    UUID adminId  = UUID.fromString("33333333-3333-3333-3333-333333333333");
    UUID sellerId = UUID.fromString("44444444-4444-4444-4444-444444444444");

    Instant now = Instant.now();

    users.save(User.builder()
        .id(aliceId).email("alice@example.com")
        .passwordHash(encoder.encode("alice123!")).role(User.Role.USER).createdAt(now).build());
    users.save(User.builder()
        .id(bobId).email("bob@example.com")
        .passwordHash(encoder.encode("bob123!")).role(User.Role.USER).createdAt(now).build());
    users.save(User.builder()
        .id(adminId).email("admin@example.com")
        .passwordHash(encoder.encode("admin123!")).role(User.Role.ADMIN).createdAt(now).build());
    users.save(User.builder()
        .id(sellerId).email("seller@example.com")
        .passwordHash(encoder.encode("seller123!")).role(User.Role.SELLER).createdAt(now).build());

    products.save(Product.builder()
        .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
        .name("Mechanical keyboard").description("Tactile switches, USB-C")
        .price(new BigDecimal("139.00")).imageUrl("/img/keyboard.png")
        .sellerId(sellerId).createdAt(now).build());
    products.save(Product.builder()
        .id(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
        .name("Wireless mouse").description("6 buttons, AA battery")
        .price(new BigDecimal("35.00")).imageUrl("/img/mouse.png")
        .sellerId(sellerId).createdAt(now).build());
    products.save(Product.builder()
        .id(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"))
        .name("27-inch monitor").description("4K IPS panel")
        .price(new BigDecimal("429.00")).imageUrl("/img/monitor.png")
        .sellerId(sellerId).createdAt(now).build());
    products.save(Product.builder()
        .id(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"))
        .name("Webcam 1080p").description("Privacy shutter included")
        .price(new BigDecimal("59.00")).imageUrl("/img/webcam.png")
        .sellerId(sellerId).createdAt(now).build());
    products.save(Product.builder()
        .id(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"))
        .name("Standing desk converter").description("Manual height adjust")
        .price(new BigDecimal("189.00")).imageUrl("/img/desk.png")
        .sellerId(sellerId).createdAt(now).build());

    log.info("Seeded {} users and {} products", users.count(), products.count());
  }
}
