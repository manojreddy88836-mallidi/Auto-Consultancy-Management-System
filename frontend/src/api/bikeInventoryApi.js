import api from './axios';

const BACKEND_BASE = 'http://localhost:8080';

// ── Public (no auth) ─────────────────────────────────────────────────────────
export const getAvailableBikes = (params) =>
    api.get('/bikes/public/available', { params });

export const getPublicBikeDetail = (id) =>
    api.get(`/bikes/public/${id}`);

export const getBikeInventoryImageUrl = (inventoryId, imageId) =>
    `${BACKEND_BASE}/api/bikes/${inventoryId}/images/${imageId}/file`;

// ── Admin / Worker ────────────────────────────────────────────────────────────
export const getBikeInventory = (params) =>
    api.get('/bikes', { params });

export const getBikeInventoryById = (id) =>
    api.get(`/bikes/${id}`);

export const createBikeInventory = (data) =>
    api.post('/bikes', data);

export const updateBikeInventory = (id, data) =>
    api.put(`/bikes/${id}`, data);

export const deleteBikeInventory = (id) =>
    api.delete(`/bikes/${id}`);

// ── Sale status transitions ───────────────────────────────────────────────────
export const enableSale = (id) =>
    api.put(`/bikes/${id}/enable-sale`);

export const disableSale = (id) =>
    api.put(`/bikes/${id}/disable-sale`);

export const reserveBike = (id) =>
    api.put(`/bikes/${id}/reserve`);

export const markSold = (id) =>
    api.put(`/bikes/${id}/sold`);

// ── Image management ─────────────────────────────────────────────────────────
export const uploadBikeInventoryImage = (inventoryId, file, setPrimary = false) => {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('setPrimary', setPrimary);
    return api.post(`/bikes/${inventoryId}/images`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
    });
};

export const getBikeInventoryImages = (inventoryId) =>
    api.get(`/bikes/${inventoryId}/images`);

export const setPrimaryBikeInventoryImage = (inventoryId, imageId) =>
    api.put(`/bikes/${inventoryId}/images/${imageId}/primary`);

export const deleteBikeInventoryImage = (inventoryId, imageId) =>
    api.delete(`/bikes/${inventoryId}/images/${imageId}`);
