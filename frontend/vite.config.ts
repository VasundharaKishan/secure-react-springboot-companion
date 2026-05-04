import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: '127.0.0.1',
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    sourcemap: false,        // See Chapter 26: source maps in prod leak code
  },
});
