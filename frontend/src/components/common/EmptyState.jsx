import React from 'react';
import Button from './Button';

export const EmptyState = ({ icon, title, description, actionLabel, onAction }) => {
  return (
    <div className="flex flex-col items-center justify-center p-8 text-center bg-gray-50 rounded-xl border border-dashed border-gray-200">
      {icon && (
        <div className="text-gray-400 mb-4 bg-white p-4 rounded-full shadow-sm">
          {icon}
        </div>
      )}
      <h3 className="text-lg font-medium text-gray-900 mb-2">{title}</h3>
      {description && <p className="text-sm text-gray-500 mb-6 max-w-sm">{description}</p>}
      {actionLabel && onAction && (
        <Button onClick={onAction}>{actionLabel}</Button>
      )}
    </div>
  );
};
export default EmptyState;
