import api from './axios';

export const getApplicationsByMonth = () => api.get('/reports/applications-by-month');
export const getApplicationsByStatus = () => api.get('/reports/applications-by-status');
export const getFinanceStats = () => api.get('/reports/finance-stats');
export const getBrandStats = () => api.get('/reports/manufacturer-stats');
export const getWorkerStats = () => api.get('/reports/applications-by-worker');
