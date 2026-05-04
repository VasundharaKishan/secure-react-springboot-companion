// FIXED — this is the SecurityFilterChain block already shipped at:
//   backend/src/main/java/com/securitybook/app/config/SecurityConfig.java
//
// Six security headers, all set via Spring Security's headers DSL:
//   1. HSTS               — forces HTTPS for 1 year, includes subdomains
//   2. CSP                — script/style/img/frame source restrictions
//   3. X-Frame-Options    — DENY blocks all iframing (clickjacking defense)
//   4. X-Content-Type-Options nosniff (Spring sets by default)
//   5. Referrer-Policy    — strict-origin-when-cross-origin (default)
//   6. (no Permissions-Policy in the baseline — add per-app if needed)

http
    // ... auth config ...
    .headers(headers -> headers
        .httpStrictTransportSecurity(hsts -> hsts
            .maxAgeInSeconds(31_536_000)              // 1 year
            .includeSubDomains(true))
        .contentSecurityPolicy(csp -> csp.policyDirectives(
            "default-src 'self'; " +
            "script-src 'self'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data:; " +
            "frame-ancestors 'none'"))                 // also blocks iframing
        .frameOptions(frame -> frame.deny())          // belt + braces
    );
