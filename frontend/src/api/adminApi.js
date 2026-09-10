import api from './axios';

export const getDashboardStats = () => api.get('/admin/dashboard');
export const getCustomers = (params) => api.get('/admin/customers', { params });
export const getCustomerById = (id) => api.get(`/admin/customers/${id}`);
export const getWorkers = (params) => api.get('/admin/workers', { params });
export const createWorker = (data) => api.post('/admin/workers', data);
export const updateWorker = (id, data) => api.put(`/admin/workers/${id}`, data);
export const deactivateWorker = (id) => api.delete(`/admin/workers/${id}`);
export const getAuditLogs = (params) => api.get('/admin/audit-logs', { params });
