import axios from './axios';

// Worker endpoints  (baseURL is already '/api', so paths start at /worker/...)
export const getMyTasks = (params = {}) =>
  axios.get('/worker/tasks', { params });

export const getTodaySummary = () =>
  axios.get('/worker/tasks/today');

export const getTaskDetail = (id) =>
  axios.get(`/worker/tasks/${id}`);

export const updateTaskStatus = (id, data) =>
  axios.put(`/worker/tasks/${id}/status`, data);

export const updateServiceJob = (id, data) =>
  axios.put(`/worker/tasks/${id}/service`, data);

export const recordPayment = (id, data) =>
  axios.post(`/worker/tasks/${id}/collect`, data);

export const updateFieldVisit = (id, data) =>
  axios.put(`/worker/tasks/${id}/visit`, data);

export const updateBikeRecovery = (id, data) =>
  axios.put(`/worker/tasks/${id}/recovery`, data);

// Admin endpoints
export const adminGetAllTasks = (params = {}) =>
  axios.get('/admin/worker-tasks', { params });

export const adminCreateTask = (data) =>
  axios.post('/admin/worker-tasks', data);

export const adminGetTaskDetail = (id) =>
  axios.get(`/admin/worker-tasks/${id}`);

export const adminUpdateTask = (id, data) =>
  axios.put(`/admin/worker-tasks/${id}`, data);

export const adminCancelTask = (id) =>
  axios.delete(`/admin/worker-tasks/${id}`);