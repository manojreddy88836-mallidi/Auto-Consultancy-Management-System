/**
 * manufacturerApi.js — DEPRECATED shim.
 *
 * The /api/manufacturers/* endpoints have been superseded by /api/brands/*.
 * This file is kept for backward compatibility only.
 * All active code should import from brandApi.js instead.
 */
import api from './axios';

/** @deprecated Use getAllBrands() from brandApi.js */
export const getAllManufacturers = () => api.get('/brands/public/all');
/** @deprecated Use getBrandsAdmin() from brandApi.js */
export const getManufacturersAdmin = (params) => api.get('/brands', { params });
/** @deprecated Use createBrand() from brandApi.js */
export const createManufacturer = (data) => api.post('/brands', data);
/** @deprecated Use updateBrand() from brandApi.js */
export const updateManufacturer = (id, data) => api.put(`/brands/${id}`, data);
/** @deprecated Use deleteBrand() from brandApi.js */
export const deleteManufacturer = (id) => api.delete(`/brands/${id}`);
/** @deprecated Use toggleBrand() from brandApi.js */
export const toggleManufacturer = (id) => api.delete(`/brands/${id}`);
