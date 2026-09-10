import React, { useState, useEffect } from 'react';
import { Search, X } from 'lucide-react';

export const SearchBar = ({ value, onChange, placeholder = 'Search...', onSearch, className = '' }) => {
  const [localValue, setLocalValue] = useState(value || '');

  // Debounce logic
  useEffect(() => {
    const timer = setTimeout(() => {
      if (onChange) onChange(localValue);
      if (onSearch) onSearch(localValue);
    }, 300);
    return () => clearTimeout(timer);
  }, [localValue, onChange, onSearch]);

  return (
    <div className={`relative ${className}`}>
      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
        <Search className="h-4 w-4 text-gray-400" />
      </div>
      <input
        type="text"
        className="block w-full pl-10 pr-10 py-2 border border-gray-300 rounded-lg leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-accent-500 focus:border-accent-500 sm:text-sm transition-all"
        placeholder={placeholder}
        value={localValue}
        onChange={(e) => setLocalValue(e.target.value)}
      />
      {localValue && (
        <button
          onClick={() => setLocalValue('')}
          className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
};
export default SearchBar;
