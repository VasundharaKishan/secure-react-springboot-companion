// FIXED — same as the policy already shipped in SecurityConfig.java.
//
// Three principles:
//   1. No 'unsafe-inline' in script-src. Inline scripts are rejected;
//      use nonces or hashes if you genuinely need them.
//   2. No 'unsafe-eval'. Defeats prototype-pollution chains.
//   3. Sources enumerated, not wildcarded. img-src 'self' data: is
//      enough for an SPA — wildcards lift the constraint entirely.

http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'; " +
        "script-src 'self'; " +
        "style-src 'self' 'unsafe-inline'; " +    // React inline styles only — see Ch 20 mitigations
        "img-src 'self' data:; " +
        "connect-src 'self'; " +
        "frame-ancestors 'none'; " +
        "base-uri 'self'; " +
        "form-action 'self'"
    ))
);

// If a vendor widget needs inline script, generate a per-request nonce:
//
//   String nonce = UUID.randomUUID().toString();
//   res.addHeader("Content-Security-Policy",
//       "script-src 'self' 'nonce-" + nonce + "'");
//
// And echo the nonce on the script tag:
//
//   <script nonce="${nonce}">/* legitimate inline */</script>
//
// Attacker-injected scripts cannot guess the nonce → still blocked.
