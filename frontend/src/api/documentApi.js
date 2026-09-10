import api from './axios';

export const uploadDocument = (applicationId, formData) => api.post(`/documents/upload/${applicationId}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } });
export const getDocuments = (applicationId) => api.get(`/documents/application/${applicationId}`);
export const updateDocumentStatus = (documentId, status, remarks) => api.put(`/documents/${documentId}/status`, { status, remarks });
export const downloadDocument = (documentId) => api.get(`/documents/${documentId}`, { responseType: 'blob' });
