import api from './axios';

// Get all EMI payments for a finance detail record
export const getEmiPayments = (financeDetailId) =>
  api.get(`/emi-payments/finance/${financeDetailId}`);

// Record a new EMI payment
export const recordEmiPayment = (data) =>
  api.post('/emi-payments', data);

// Update an existing payment record
export const updateEmiPayment = (id, data) =>
  api.put(`/emi-payments/${id}`, data);

// Delete a payment record
export const deleteEmiPayment = (id) =>
  api.delete(`/emi-payments/${id}`);

// Manually trigger overdue recalculation for all financed records
export const triggerOverdueRefresh = () =>
  api.post('/emi-payments/refresh-overdue');
