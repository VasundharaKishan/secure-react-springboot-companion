import axios from 'axios';

/**
 * Axios instance shared by every page.
 *
 * The baseURL falls back to the Vite dev proxy (`/api`) so requests work
 * during local development without CORS. In production builds the
 * VITE_API_BASE_URL env var supplies the full origin.
 *
 * Authentication: the access token lives in memory only (see Chapter 8 and
 * Chapter 14). The refresh token is delivered as an HttpOnly cookie that the
 * browser attaches automatically — we do not read it from JS.
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  withCredentials: true,
});

let accessToken: string | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});
