import api from './axios';

export const getCustomerProfile    = ()     => api.get('/customers/profile');
export const updateCustomerProfile = (data) => api.put('/customers/profile', data);
export const getCustomerDashboard  = ()     => api.get('/customers/dashboard');