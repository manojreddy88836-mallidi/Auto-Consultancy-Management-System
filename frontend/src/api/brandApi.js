import api from './axios';

export const getAllBrands = () => api.get('/brands/public/all');
export const getBrandsAdmin = (params) => api.get('/brands', { params });
export const createBrand = (data) => api.post('/brands', data);
export const updateBrand = (id, data) => api.put(`/brands/${id}`, data);
export const deleteBrand = (id) => api.delete(`/brands/${id}`);
// toggle = deactivate (DELETE) in the backend
export const toggleBrand = (id) => api.delete(`/brands/${id}`);
