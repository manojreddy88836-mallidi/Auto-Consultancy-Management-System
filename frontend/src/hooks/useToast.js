import { toast } from 'react-hot-toast';

export const useToast = () => {
  return {
    success: (message) => toast.success(message, { style: { background: '#10B981', color: '#fff' } }),
    error: (message) => toast.error(message, { style: { background: '#EF4444', color: '#fff' } }),
    warning: (message) => toast(message, { icon: '⚠️', style: { background: '#F59E0B', color: '#fff' } }),
    info: (message) => toast(message, { icon: 'ℹ️', style: { background: '#3B82F6', color: '#fff' } }),
  };
};
