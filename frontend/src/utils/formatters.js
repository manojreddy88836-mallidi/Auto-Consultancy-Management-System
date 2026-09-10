import { format, formatDistanceToNow } from 'date-fns';

export const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  try { return format(new Date(dateStr), 'dd MMM yyyy'); } catch { return dateStr; }
};

export const formatDateTime = (dateStr) => {
  if (!dateStr) return 'N/A';
  try { return format(new Date(dateStr), 'dd MMM yyyy, hh:mm a'); } catch { return dateStr; }
};

export const formatRelative = (dateStr) => {
  if (!dateStr) return 'N/A';
  try { return formatDistanceToNow(new Date(dateStr), { addSuffix: true }); } catch { return dateStr; }
};

export const formatCurrency = (amount) => {
  if (amount == null) return '₹0';
  return `₹${Number(amount).toLocaleString('en-IN')}`;
};

export const formatFileSize = (bytes) => {
  if (!bytes) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  let size = bytes;
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++; }
  return `${size.toFixed(1)} ${units[i]}`;
};

export const getInitials = (name) => {
  if (!name) return '??';
  return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
};

export const truncateText = (text, maxLength = 50) => {
  if (!text) return '';
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
};

export const getStatusLabel = (status) => {
  const map = {
    DRAFT: 'Draft', SUBMITTED: 'Submitted', UNDER_REVIEW: 'Under Review',
    DOCUMENT_VERIFICATION: 'Doc. Verification', FINANCE_VERIFICATION: 'Finance Verification',
    WORKER_ASSIGNED: 'Worker Assigned', APPROVED: 'Approved', REJECTED: 'Rejected', COMPLETED: 'Completed'
  };
  return map[status] || status;
};
