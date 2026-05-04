# Backend — Spring Boot 3.4 + Java 21

Reference application for the security book. Implements a small e-commerce API
with users, products, and orders so chapters can demonstrate vulnerabilities
in a realistic context.

## Run

```bash
# From the repo root: start Postgres + Redis first
docker compose up -d

# From backend/
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

## Switching between vulnerable and fixed

Some chapters use the `SECURITY_MODE` env var:

```bash
# Demonstrate the broken behavior the book describes
SECURITY_MODE=vulnerable ./mvnw spring-boot:run

# Demonstrate the corrected behavior (default)
SECURITY_MODE=fixed ./mvnw spring-boot:run
```

Other chapters require you to swap a file. See `chapters/NN-topic/README.md`
for instructions.

## Versions

See `pom.xml`. Pinned to:

- Spring Boot 3.4.1
- Spring Security 6.4.x (transitive)
- JJWT 0.12.6
- Hibernate 6.x (Jakarta)
- Bucket4j 8.10.1
- Postgres driver 42.x

## Tests

```bash
./mvnw test
```

Security regression tests live in `src/test/java/com/securitybook/app/security/`
and assert that fixed implementations block their corresponding attacks.

## Structure

```
src/main/java/com/securitybook/app/
├── SecurityBookApplication.java
├── config/                    Cross-cutting beans (security, CORS, Jackson)
├── security/                  Auth filters, JWT provider, current-user resolver
├── user/                      User aggregate (registration, login, profile)
├── product/                   Product catalog (search, image upload)
├── order/                     Orders (placement, history, admin views)
└── ...
```
