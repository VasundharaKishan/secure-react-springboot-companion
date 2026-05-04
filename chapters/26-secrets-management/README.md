# Chapter 26 — Secrets Management (worked examples)

> **Status:** README and stub directories only. Code coming in a future
> revision; see Chapter 17 for a fully implemented example to mirror.

## What this chapter demonstrates

This chapter ships vulnerable + fixed implementations for the following scenarios:

- AWS keys in application.yml
- .env.production in React bundle
- Vault token in Dockerfile
- JWT signing key in git history
- DB password in startup logs

## How to use

Each subdirectory will follow the same pattern as Chapter 17:

```
26-secrets-management/
├── README.md           ← this file (with attack reproduction steps)
├── vulnerable/         ← drop-in files showing the broken implementations
├── fixed/              ← corrected versions
└── attack.sh           ← runnable attack against the vulnerable version
```

## Reproducing the attacks

For each scenario above, the eventual workflow is:

1. Copy `vulnerable/<scenario>.java` (or `.tsx`) into the corresponding
   `backend/` or `frontend/` location
2. Restart the affected service
3. Run `./attack.sh` (or `./attack-<scenario>.sh` if multiple)
4. Replace with `fixed/<scenario>.java`
5. Re-run the attack — confirm it now fails

## Contributing

If you'd like to flesh out this chapter's examples ahead of the official
release, follow the Chapter 17 pattern and open a PR. Each scenario should
include:

- A realistic `vulnerable/` file (the kind of code a real developer writes)
- The `fixed/` counterpart with the minimum diff to close the bug
- An `attack.sh` (or `.py`) that proves the attack works on vulnerable
  and fails on fixed
- A regression test under
  `backend/src/test/java/com/securitybook/app/security/` (or equivalent
  Vitest test under `frontend/src/__tests__/security/`) that pins the fix

Cross-reference Chapter 26 of the book for the conceptual explanation
behind each scenario.
