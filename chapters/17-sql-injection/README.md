# Chapter 17 — SQL Injection (worked example)

This chapter ships a deliberately broken product-search endpoint and the
corrected version. You will:

1. Run the vulnerable version
2. Exfiltrate every user's email and password hash via UNION-based injection
3. Swap in the fixed version
4. Re-run the same attack and watch it return zero rows

## Prerequisites

- Backend running (`./mvnw spring-boot:run` from `backend/`)
- Postgres seeded (the app does this automatically on first start via `seed.sql`)

## Step 1 — Install the vulnerable controller

Copy the file:

```bash
cp chapters/17-sql-injection/vulnerable/ProductSearchController.java \
   backend/src/main/java/com/securitybook/app/product/ProductSearchController.java
```

Restart the backend. The endpoint is now:

```
GET /api/products/search?q=<query>
```

## Step 2 — Confirm the endpoint works (benign request)

```bash
curl -s 'http://localhost:8080/api/products/search?q=keyboard' | jq
```

You should see a JSON array of products whose name contains "keyboard".

## Step 3 — Run the attack

```bash
./chapters/17-sql-injection/attack.sh
```

What this script does:

1. Sends a benign query to confirm baseline behavior
2. Sends a UNION-based payload that appends every user's email + password hash
   to the result set, dressed up as fake products
3. Pretty-prints the leaked rows

The crucial payload is:

```
'  UNION  SELECT  NULL::uuid, email||':'||password_hash, NULL::numeric, NULL FROM users  --
```

NULL literals are type-flexible in Postgres, so the attacker doesn't need to
know the exact schema of `products` to mount a UNION attack — they only need
to fill the column they want to exfiltrate.

You'll see every seeded user's bcrypt hash returned in what looks like
the product list. That hash is offline-crackable — see Chapter 7.

## Step 4 — Apply the fix

```bash
cp chapters/17-sql-injection/fixed/ProductSearchController.java \
   backend/src/main/java/com/securitybook/app/product/ProductSearchController.java
```

Restart the backend.

## Step 5 — Re-run the attack

```bash
./chapters/17-sql-injection/attack.sh
```

The injection payload now returns an empty list — Postgres treats the entire
string (UNION and all) as the literal value of the `:search` parameter,
which never matches any product name.

## What changed in the fix

```diff
- String sql = "SELECT id, name, price, image_url FROM products " +
-              "WHERE name LIKE '%" + query + "%' ORDER BY name LIMIT 50";
- return jdbcTemplate.query(sql, productRowMapper);
+ String sql = "SELECT id, name, price, image_url FROM products " +
+              "WHERE name ILIKE :search ORDER BY name LIMIT 50";
+ return namedJdbc.query(sql, Map.of("search", "%" + query + "%"), productRowMapper);
```

Two things matter:

1. The `?` (or named) placeholder turns the value into a **parameter** that
   the driver sends separately from the SQL statement. Postgres never
   re-parses the parameter as SQL — there is no syntactic surface for
   injection.
2. The `%` wildcards live in the application code, not in the user input.
   The attacker cannot smuggle wildcards as injection.

## Verification test

A regression test exists at:

```
backend/src/test/java/com/securitybook/app/product/ProductSearchSqlInjectionTest.java
```

Run with:

```bash
./mvnw test -Dtest=ProductSearchSqlInjectionTest
```

It asserts that the fixed endpoint returns zero rows for the UNION payload
and that the vulnerable endpoint returns user data — the test pins the
behavior so a regression cannot ship.

## What attackers try next

Even with parameterized queries, attackers will probe:

- **Dynamic ORDER BY** with column injection (covered in `vulnerable/SortInjection.md`)
- **Stored procedures** that re-construct SQL inside the database
- **Second-order injection** where input is safely stored and later re-used in
  unsafe contexts (Chapter 17, "Second-order SQLi" section)
- **Database error messages** leaking in stack traces (Chapter 22)
