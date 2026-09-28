import axios from 'axios';

const STORAGE_TOKEN_KEY = 'ac_token';

const api = axios.create({
  baseURL: '/api', // Vite proxy handles this
  headers: { 'Content-Type': 'application/json' },
  // Do NOT send cookies cross-origin (CSRF mitigation on top of stateless JWT)
  withCredentials: false,
});

// ── Request interceptor: attach JWT from sessionStorage ─────────────────────
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(STORAGE_TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ── Response interceptor: handle auth failures ───────────────────────────────
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
