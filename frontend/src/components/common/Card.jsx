import React from 'react';

export const Card = ({ title, subtitle, children, actions, padding = 'p-6', className = '' }) => {
  return (
    <div className={`bg-white rounded-xl shadow-sm border border-slate-100 hover:shadow-md transition-shadow duration-200 ${className}`}>
      {(title || actions) && (
        <div className={`px-6 py-4 border-b border-slate-100 flex items-center justify-between`}>
          <div>
            {title && <h3 className="text-lg font-semibold text-slate-800">{title}</h3>}
            {subtitle && <p className="text-sm text-slate-500 mt-1">{subtitle}</p>}
          </div>
          {actions && <div>{actions}</div>}
        </div>
      )}
      <div className={padding}>
        {children}
      </div>
    </div>
  );
};
export default Card;
