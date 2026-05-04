# Companion Code — *Security in Frontend and Backend Applications*

Runnable code examples for the book. Every chapter that demonstrates a vulnerability
has both the **vulnerable** version (so you can exploit it yourself) and the **fixed**
version (so you can verify the defense holds).

> **Read the book alongside this repo.** The book explains *why*. This repo lets you
> see *how* — including the satisfaction of running the attack and watching it succeed,
> then watching it fail after the fix.

## Quick start

You need:

- **Java 21** (Temurin recommended)
- **Node 22+** (or use the included `.nvmrc`)
- **Docker + docker-compose** (for Postgres, Redis, and a few sidecars)

```bash
# 1. Clone
git clone https://github.com/YOUR_USERNAME/security-book-code.git
cd security-book-code

# 2. Start dependencies (Postgres on 5432, Redis on 6379)
docker compose up -d

# 3. Run the backend
cd backend && ./mvnw spring-boot:run
# API now on http://localhost:8080

# 4. In another terminal, run the frontend
cd frontend && npm install && npm run dev
# UI now on http://localhost:5173
```

## How chapters are organized

Each chapter that demonstrates a vulnerability has its own directory:

```
chapters/
├── 17-sql-injection/
│   ├── README.md          ← Setup + how to reproduce attack
│   ├── vulnerable/        ← Drop these files into the app
│   └── fixed/             ← The corrected versions
│   └── attack.sh          ← The exact attack from the book
└── ...
```

To work through a chapter:

1. Open `chapters/NN-topic/README.md`
2. Copy files from `vulnerable/` into the corresponding `backend/` or `frontend/`
   locations (paths are listed at the top of each file)
3. Restart the affected service
4. Run `./attack.sh` — watch it succeed
5. Replace the vulnerable files with `fixed/` versions
6. Restart and re-run the attack — watch it fail

Some chapters use a **switch** instead of file replacement. Look for
`SECURITY_MODE=vulnerable` or `SECURITY_MODE=fixed` in the chapter README.

## Repository layout

```
.
├── backend/                   Spring Boot 3.4 + Java 21 reference app
│   ├── src/main/java/...      Common modules: user, product, order, auth
│   ├── src/main/resources/    application.yml, schema.sql, seed data
│   └── pom.xml
│
├── frontend/                  React 19 + Vite 6 + TypeScript
│   ├── src/
│   └── package.json
│
├── chapters/                  Per-chapter vulnerable/fixed pairs
│   ├── 05-authentication/
│   ├── 06-spring-security-config/
│   ├── ...
│   └── 31-logging-monitoring/
│
├── scripts/attack/            Reusable attack helpers (curl, python, jwt)
│
├── docker-compose.yml         Postgres, Redis, MailHog, ClamAV
└── docs/                      Architecture diagrams, threat model
```

## Reference application

The backend and frontend implement a small e-commerce app:

- **Users** can register, log in, browse products, place orders
- **Admins** can manage products and view all orders
- **Sellers** can manage their own products and see their order history

This deliberately spans common attack surfaces: authentication, role-based access,
search (SQL), file upload (product images), payments (rate-limit-sensitive),
WebSocket (order notifications), and more — so every chapter has somewhere realistic
to demonstrate its vulnerability.

## Safety

The vulnerable code in this repo is **deliberately broken**. Do not deploy it.
Do not point it at real data. Run only on `localhost`. The `docker-compose.yml`
binds services to `127.0.0.1` only.

If you want to share a vulnerable example with someone (workshops, demos),
ngrok / localtunnel are fine for short windows but never leave it exposed.

## Versions pinned

| Component         | Version       |
|-------------------|---------------|
| Java              | 21 LTS        |
| Spring Boot       | 3.4.x         |
| Spring Security   | 6.4.x         |
| JJWT              | 0.12.x        |
| Hibernate         | 6.x (Jakarta) |
| React             | 19.x          |
| Vite              | 6.x           |
| TypeScript        | 5.x           |
| TanStack Query    | 5.x           |
| React Router      | 7.x           |
| React Hook Form   | 7.x           |
| Zod               | 3.x           |
| Postgres          | 16            |
| Redis             | 7             |

See `backend/pom.xml` and `frontend/package.json` for exact versions.

## License

MIT for the code. The book text remains under its own copyright.

## Contributing

If you find a bug in the **fixed** version, please open an issue — that means the
defense is incomplete and should be hardened. If you find a *new* attack vector
on the vulnerable version, please open an issue or PR with the new arc treatment.

Pattern for adding a new chapter example:

1. Add `chapters/NN-topic/` with `vulnerable/`, `fixed/`, and `README.md`
2. Reference the file paths in the parent `backend/` or `frontend/` tree
3. Include an `attack.sh` (or `attack.py`) that's runnable in one command
4. Add an integration test in `backend/src/test/.../security/` that asserts the
   fixed version blocks the attack

## Errata

Found something wrong in the book or repo? Open an issue tagged `errata`.
