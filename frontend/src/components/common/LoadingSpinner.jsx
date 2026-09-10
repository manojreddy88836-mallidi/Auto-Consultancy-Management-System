import React from 'react';

export const LoadingSpinner = ({ fullScreen = false, size = 'md', text = 'Loading...' }) => {
  const sizeClasses = {
    sm: 'w-4 h-4 border-2',
    md: 'w-8 h-8 border-3',
    lg: 'w-12 h-12 border-4',
  };

  const spinner = (
    <div className="flex flex-col items-center justify-center gap-3">
      <div className={`${sizeClasses[size]} border-gray-200 border-t-accent-500 rounded-full animate-spin`}></div>
      {text && <span className="text-gray-500 text-sm font-medium">{text}</span>}
    </div>
  );

  if (fullScreen) {
    return (
      <div className="fixed inset-0 bg-white bg-opacity-75 backdrop-blur-sm z-50 flex items-center justify-center">
        {spinner}
      </div>
    );
  }

  return <div className="py-8">{spinner}</div>;
};
export default LoadingSpinner;
