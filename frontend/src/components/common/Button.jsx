import React from 'react';

const variants = {
  primary: 'bg-accent-500 hover:bg-accent-700 text-white',
  secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-700',
  danger: 'bg-red-500 hover:bg-red-700 text-white',
  ghost: 'bg-transparent hover:bg-gray-100 text-gray-700',
  outline: 'border-2 border-accent-500 text-accent-500 hover:bg-accent-50',
};

const sizes = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-6 py-3 text-lg',
};

export const Button = ({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  icon,
  className = '',
  disabled,
  ...props
}) => {
  return (
    <button
      className={`inline-flex items-center justify-center font-medium rounded-lg transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98]
        ${variants[variant]} ${sizes[size]} 
        ${disabled || loading ? 'opacity-50 cursor-not-allowed hover:scale-100' : ''} 
        ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading && (
        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      )}
      {!loading && icon && <span className="mr-2">{icon}</span>}
      {children}
    </button>
  );
};
export default Button;
