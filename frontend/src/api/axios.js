import axios from 'axios';

const STORAGE_TOKEN_KEY = 'ac_token';

// ── Base URL resolution ───────────────────────────────────────────────────────
// Priority:
//  1. VITE_API_BASE_URL env var (set in Render dashboard for production)
//     e.g. https://auto-consultancy-backend.onrender.com
//  2. '' (empty string) → Vite proxy handles /api → localhost:8080 in dev
//
// In production (Render static site), there is NO Vite proxy.
// The env var MUST be set to the backend Render URL in the frontend service.
const BASE_URL = import.meta.env.VITE_API_BASE_URL
  ? `${import.meta.env.VITE_API_BASE_URL}/api`
  : '/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  // Do NOT send cookies cross-origin (CSRF mitigation on top of stateless JWT)
  withCredentials: false,
});

// ── Request interceptor: attach JWT from sessionStorage ───────────────────────
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(STORAGE_TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ── Response interceptor: handle auth failures ────────────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      // Token expired or revoked — clear local state and force re-login
      sessionStorage.removeItem(STORAGE_TOKEN_KEY);
      localStorage.removeItem('ac_user');
      // Clean up any legacy keys
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
