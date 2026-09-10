import api from './axios';

export const getWorkerDashboard = () => api.get('/worker/dashboard');
export const getWorkerProfile = () => api.get('/worker/profile');
