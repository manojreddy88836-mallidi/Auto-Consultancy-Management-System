import axios from 'axios';

const api = axios.create({
  baseURL: '/api', // Vite proxy handles this
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status === 401 || status === 403) {
      const isAdminPath = window.location.pathname.startsWith('/admin') ||
                          window.location.pathname.startsWith('/worker');
      // Only auto-redirect on 403 from protected (admin/worker) paths
      // to avoid redirect loops from public endpoints
      if (status === 401 || (status === 403 && isAdminPath)) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
