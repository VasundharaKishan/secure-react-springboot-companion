# Frontend — React 19 + Vite 6 + TypeScript

The companion React app for the security book. Talks to the Spring Boot backend
on `http://localhost:8080` via a Vite dev proxy.

## Run

```bash
npm install
npm run dev
```

App opens at `http://localhost:5173`.

## Stack

- React 19 (function components, hooks, Actions API where it fits)
- Vite 6 + TypeScript 5
- React Router 7 (data router pattern)
- TanStack Query 5 (server state)
- React Hook Form 7 + Zod 3 (forms)
- Axios 1 (HTTP, with in-memory access token + httpOnly refresh cookie)
- DOMPurify (sanitization for the few places where rendered HTML is unavoidable)
- MSW 2 (mocking in tests)
- Vitest 2 + RTL 16

## Env vars

Copy `.env.example` to `.env.local`. Only variables prefixed with `VITE_`
are exposed to the browser bundle — see Chapter 26 for what happens when
you forget that.

## Chapter integration

Per-chapter examples live in `../chapters/NN-topic/`. Each chapter README
explains which file to swap in and what attack to run after.
