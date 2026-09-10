import api from './axios';

// bikeInventoryId is the PRIMARY reference (exact physical bike from inventory)
// bikeModelId is the LEGACY fallback for backward compatibility
export const createApplication = (bikeInventoryId = null, bikeModelId = null) => {
    const body = {};
    if (bikeInventoryId) body.bikeInventoryId = bikeInventoryId;
    if (bikeModelId)     body.bikeModelId = bikeModelId;
    return api.post('/applications', body);
};

export const getMyApplications     = ()     => api.get('/applications/my');
export const getApplicationById    = (id)   => api.get(`/applications/${id}`);
export const submitApplication     = (id)   => api.put(`/applications/${id}/submit`);
export const getAllApplicationsAdmin = (params) => api.get('/applications', { params });
export const getAssignedApplications = ()   => api.get('/applications/assigned');
export const updateApplicationStatus = (id, data) => api.put(`/applications/${id}/status`, data);
export const assignWorker = (id, workerId) => api.put(`/applications/${id}/assign-worker`, { workerId });
export const updateBikeDetails    = (id, data) => api.put(`/applications/${id}/bike-details`, data);
export const updateFinanceDetails = (id, data) => api.put(`/applications/${id}/finance-details`, data);
export const deleteApplication    = (id)        => api.delete(`/applications/${id}`);
