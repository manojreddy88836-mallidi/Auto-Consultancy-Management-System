import React from 'react';

export const StatsCard = ({ icon, title, value, change, changeType, color = 'blue' }) => {
  const colorClasses = {
    blue: 'bg-blue-50 text-blue-600',
    indigo: 'bg-indigo-50 text-indigo-600',
    primary: 'bg-primary-50 text-primary-600',
    orange: 'bg-orange-50 text-orange-600',
  };
  return (
    <div className="bg-white rounded-xl shadow-sm hover:shadow-md transition-all duration-200 p-6 border border-slate-100">
      <div className="flex items-center justify-between">
        <div className={`p-3 rounded-lg ${colorClasses[color] || colorClasses.blue}`}>
          {icon}
        </div>
        {change && (
          <span className={`text-sm font-medium ${changeType === 'positive' ? 'text-green-600' : 'text-red-600'}`}>
            {change}
          </span>
        )}
      </div>
      <div className="mt-4">
        <h3 className="text-slate-500 text-sm font-medium">{title}</h3>
        <p className="text-2xl font-bold text-slate-800 mt-1">{value}</p>
      </div>
    </div>
  );
};
export default StatsCard;
