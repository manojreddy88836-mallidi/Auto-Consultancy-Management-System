import api from './axios';

// ── Public (no auth) ────────────────────────────────────────────────────────
export const getModelsByManufacturer  = (manufacturerId) => api.get(`/bike-models/public/by-manufacturer/${manufacturerId}`);
export const getVariantsByModel       = (modelId)        => api.get(`/bike-models/public/${modelId}/variants`);
export const getYearsByModel          = (modelId)        => api.get(`/bike-models/public/${modelId}/years`);
export const getAvailableBikesForSale = (params)         => api.get('/bike-models/public/for-sale', { params });
export const getPublicBikeDetail      = (id)             => api.get(`/bike-models/public/${id}/detail`);

// Build image URL for display (public, no auth)
export const getBikeImageUrl = (imageId) => `/api/bike-models/images/${imageId}/file`;

// ── Admin – Bike Models ─────────────────────────────────────────────────────
export const getBikeModelsAdmin = (params) => api.get('/bike-models', { params });
export const createBikeModel    = (data)   => api.post('/bike-models', data);
export const updateBikeModel    = (id, data) => api.put(`/bike-models/${id}`, data);
export const deleteBikeModel    = (id)     => api.delete(`/bike-models/${id}`);

// ── Admin/Worker – Sale status ──────────────────────────────────────────────
export const updateSaleStatus = (id, saleStatus) =>
    api.put(`/bike-models/${id}/sale-status`, { saleStatus });

// ── Admin/Worker – Images ───────────────────────────────────────────────────
export const uploadBikeImage    = (modelId, file, setPrimary = false) => {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('setPrimary', setPrimary);
    return api.post(`/bike-models/${modelId}/images`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
    });
};
export const getBikeImages      = (modelId)          => api.get(`/bike-models/${modelId}/images`);
export const deleteBikeImage    = (modelId, imageId) => api.delete(`/bike-models/${modelId}/images/${imageId}`);
export const setPrimaryBikeImage= (modelId, imageId) => api.put(`/bike-models/${modelId}/images/${imageId}/primary`);

// ── Admin – Variants / Years ────────────────────────────────────────────────
export const addVariant            = (modelId, data)    => api.post(`/bike-models/${modelId}/variants`, data);
export const updateVariant         = (variantId, data)  => api.put(`/bike-models/variants/${variantId}`, data);
export const deleteVariant         = (variantId)        => api.delete(`/bike-models/variants/${variantId}`);
export const addManufacturingYear  = (modelId, data)    => api.post(`/bike-models/${modelId}/years`, data);
export const deleteManufacturingYear = (yearId)         => api.delete(`/bike-models/years/${yearId}`);
