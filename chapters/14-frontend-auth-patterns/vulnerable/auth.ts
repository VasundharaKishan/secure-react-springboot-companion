// VULNERABLE — copy to:
//   frontend/src/lib/api.ts (replacing the access token storage section)
//
// The bug: the JWT lives in localStorage. Any script running in the
// page context can read it — and any successful XSS turns into account
// takeover, because the attacker can extract the token and use it from
// their own machine for the rest of its TTL.
//
// localStorage is the wrong place for a credential. It is:
//   - Synchronous, JS-readable, persistent across tabs and reloads
//   - Visible in DevTools without auth
//   - Indexed by some browser extensions
//   - Not protected by SameSite, CSP script-src, or any other browser policy

import axios from 'axios';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
});

const TOKEN_KEY = 'auth.token';

// BUG: persistent, JS-readable storage of a bearer credential.
export function setAccessToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else       localStorage.removeItem(TOKEN_KEY);
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
