// FIXED — same as the file shipped at:
//   frontend/src/lib/api.ts
//
// Two changes from the vulnerable version:
//   1. The access token lives in a module-scoped variable. JS in the
//      same bundle can read it, but it is not persisted. A successful
//      XSS still gets the in-memory token, but the token's window
//      narrows to the current tab's lifetime instead of "forever."
//   2. The refresh token is delivered as an HttpOnly Secure SameSite=Lax
//      cookie that JavaScript cannot read. Even if XSS exfiltrates the
//      access token, it cannot mint new ones once the access token expires.
//
// This is the "BFF" / token-handler pattern recommended by current OAuth
// best-current-practice guidance.

import axios from 'axios';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  withCredentials: true,        // browser sends the refresh cookie automatically
});

let accessToken: string | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

// On 401, attempt one silent refresh. The refresh endpoint reads the
// HttpOnly cookie that XSS cannot touch and returns a new short-lived
// access token.
api.interceptors.response.use(
  (r) => r,
  async (err) => {
    if (err.response?.status !== 401 || err.config?._retried) throw err;
    err.config._retried = true;
    try {
      const r = await api.post('/api/auth/refresh');
      setAccessToken(r.data.token);
      err.config.headers.Authorization = `Bearer ${r.data.token}`;
      return api.request(err.config);
    } catch {
      setAccessToken(null);
      throw err;
    }
  },
);
