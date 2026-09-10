import api from './axios';

export const getNotifications = (page = 0) => api.get('/notifications', { params: { page } });
export const markAsRead = (id) => api.put(`/notifications/${id}/read`);
export const markAllAsRead = () => api.put('/notifications/read-all');
export const getUnreadCount = () => api.get('/notifications/unread-count');
