import React from 'react';
import Modal from './Modal';
import Button from './Button';
import { AlertCircle, AlertTriangle, Info } from 'lucide-react';

export const ConfirmDialog = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  variant = 'danger'
}) => {
  const getIcon = () => {
    switch (variant) {
      case 'danger': return <AlertCircle className="text-red-500 w-10 h-10" />;
      case 'warning': return <AlertTriangle className="text-amber-500 w-10 h-10" />;
      default: return <Info className="text-blue-500 w-10 h-10" />;
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="sm" title={title}>
      <div className="flex flex-col items-center text-center">
        <div className="mb-4 bg-gray-50 p-4 rounded-full">
          {getIcon()}
        </div>
        <p className="text-gray-600 mb-6">{message}</p>
        <div className="flex gap-3 w-full justify-center">
          <Button variant="secondary" onClick={onClose} className="w-full">
            {cancelText}
          </Button>
          <Button variant={variant === 'warning' ? 'primary' : variant} onClick={onConfirm} className="w-full">
            {confirmText}
          </Button>
        </div>
      </div>
    </Modal>
  );
};
export default ConfirmDialog;
