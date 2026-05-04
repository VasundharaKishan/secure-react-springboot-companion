// VULNERABLE — copy to:
//   backend/src/main/java/com/securitybook/app/config/SecurityHeadersConfig.java
//
// You also need to comment out the .headers(...) section in the canonical
// SecurityConfig.java so this empty config wins (or just remove the
// .headers(...) call from SecurityConfig). The chapter README walks through
// the swap.
//
// The bug: no security headers. The page can be iframed by anyone, sniff
// against Content-Type is on, no HSTS so a downgrade attack works.
//
// Most visible result: clickjacking. An attacker iframes your bank-transfer
// page on an innocent-looking site, overlays opaque buttons, the user clicks
// "Win prize!" but actually clicks "Send $1000."
package com.securitybook.app.config;

// Empty marker class — the absence of headers configuration in
// SecurityConfig is the bug. See chapters/21-security-headers/README.md for
// which lines to remove from SecurityConfig.
public class SecurityHeadersConfig {
  // intentionally empty
}
