// VULNERABLE — modify the .headers(...) section in:
//   backend/src/main/java/com/securitybook/app/config/SecurityConfig.java
//
// Find the contentSecurityPolicy(...) call and replace its policy string
// with the version below. The bug: 'unsafe-inline' allows any <script>
// tag in the page to execute, defeating the entire purpose of CSP. An
// XSS payload that React's escaping might block is now executable.
//
// Real-world examples of unsafe-inline-induced breaches:
// - GitHub disabled it from their primary domains in 2018 after CSP
//   exfiltrated multiple bugs
// - Twitter shipped unsafe-inline through 2020; XSS-class issues
//   persisted until they removed it
//
// The most common reason developers add unsafe-inline: a third-party
// analytics or chat widget that injects inline scripts. The right
// answer is to use nonces or hashes, not to weaken the policy globally.

http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'; " +
        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +   // ← BUG
        "style-src 'self' 'unsafe-inline'; " +
        "img-src *; " +                                          // ← BUG: wildcard
        "frame-ancestors 'none'"
    ))
);
