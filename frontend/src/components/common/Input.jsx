import React from 'react';

export const Input = ({
  label,
  error,
  icon,
  rightIcon,
  type = 'text',
  register,
  className = '',
  ...props
}) => {
  return (
    <div className="w-full">
      {label && <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>}
      <div className="relative">
        {icon && (
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
            {icon}
          </div>
        )}
        <input
          type={type}
          className={`w-full border rounded-lg px-4 py-2.5 outline-none transition-shadow
            ${error ? 'border-red-500 focus:ring-2 focus:ring-red-200' : 'border-gray-300 focus:ring-2 focus:ring-accent-500'}
            ${icon ? 'pl-10' : ''} ${rightIcon ? 'pr-10' : ''} ${className}`}
          {...(register ? register : {})}
          {...props}
        />
        {rightIcon && (
          <div className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400">
            {rightIcon}
          </div>
        )}
      </div>
      {error && <p className="mt-1 text-sm text-red-500">{error.message || error}</p>}
    </div>
  );
};
export default Input;
