# Vulnerable state

There is no "vulnerable file" to copy in. The vulnerability is the
**absence** of `LoginRateLimitFilter`. To reproduce the broken state:

1. Delete (or rename) the canonical filter:

   ```bash
   mv backend/src/main/java/com/securitybook/app/security/LoginRateLimitFilter.java \
      backend/src/main/java/com/securitybook/app/security/LoginRateLimitFilter.java.disabled
   ```

2. Rebuild and restart:

   ```bash
   cd backend && mvn -DskipTests package && java -jar target/security-book-app-0.1.0.jar
   ```

3. Run the attack — every request will return 401, never 429:

   ```bash
   bash chapters/23-rate-limiting/attack.sh
   ```

## Why "do nothing" is the bug

`/api/auth/login` accepts a JSON body with email + password and returns 200
on success or 401 on failure. Without rate limiting:

- An attacker holding a leaked credential dump (millions of email/password
  pairs from past breaches) can try every combo
- Spring Boot will happily process thousands of attempts per second per
  client
- BCrypt's intentional slowness only adds milliseconds — not enough to
  matter at scale
- The server logs fill with 401s but no alert fires unless you separately
  configure failed-login monitoring (Chapter 31)

This is **credential stuffing**, the #1 cause of account takeover today.

## Restore

```bash
mv backend/src/main/java/com/securitybook/app/security/LoginRateLimitFilter.java.disabled \
   backend/src/main/java/com/securitybook/app/security/LoginRateLimitFilter.java
cd backend && mvn -DskipTests package && java -jar target/security-book-app-0.1.0.jar
```
