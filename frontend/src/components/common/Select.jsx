import React from 'react';

export const Select = ({
  label,
  error,
  options = [],
  register,
  className = '',
  placeholder = 'Select an option',
  ...props
}) => {
  return (
    <div className="w-full">
      {label && <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>}
      <select
        className={`w-full border rounded-lg px-4 py-2.5 outline-none bg-white transition-shadow
          ${error ? 'border-red-500 focus:ring-2 focus:ring-red-200' : 'border-gray-300 focus:ring-2 focus:ring-accent-500'}
          ${className}`}
        {...(register ? register : {})}
        {...props}
      >
        <option value="" disabled hidden>{placeholder}</option>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
      {error && <p className="mt-1 text-sm text-red-500">{error.message || error}</p>}
    </div>
  );
};
export default Select;
