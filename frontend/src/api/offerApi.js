import api from './axios';

const BASE = '/offers';

// ── Customer ──────────────────────────────────────────────────────────────

/** Submit a new price offer for a specific inventory bike */
export const submitOffer = (data) => api.post(BASE, data);

/** Get all offers made by the authenticated customer */
export const getMyOffers = () => api.get(`${BASE}/my`);

/** Get my latest offer on a specific inventory bike (for BikeDetailPage) */
export const getMyOfferForBike = (bikeInventoryId) =>
    api.get(`${BASE}/my/bike/${bikeInventoryId}`);

/** Customer withdraws a pending or counter offer */
export const withdrawOffer = (id) => api.put(`${BASE}/${id}/withdraw`);

/** Customer responds to a counter offer — accept=true/false */
export const respondToCounter = (id, accept) =>
    api.put(`${BASE}/${id}/counter-response`, null, { params: { accept } });

// ── Admin / Worker ────────────────────────────────────────────────────────

/** Paginated list of all offers — admin / worker only */
export const getAllOffers = (params) => api.get(BASE, { params });

/** Admin/worker responds: ACCEPTED, REJECTED, or COUNTER_OFFER */
export const respondToOffer = (id, data) => api.put(`${BASE}/${id}/respond`, data);

/** Get a single offer by ID */
export const getOfferById = (id) => api.get(`${BASE}/${id}`);
