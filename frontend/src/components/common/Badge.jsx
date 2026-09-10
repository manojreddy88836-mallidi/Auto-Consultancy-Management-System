import React from 'react';
import { APPLICATION_STATUSES } from '../../utils/constants';

const colors = {
  gray: 'bg-gray-100 text-gray-800 border-gray-200',
  blue: 'bg-blue-100 text-blue-800 border-blue-200',
  yellow: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  orange: 'bg-orange-100 text-orange-800 border-orange-200',
  purple: 'bg-purple-100 text-purple-800 border-purple-200',
  indigo: 'bg-indigo-100 text-indigo-800 border-indigo-200',
  green: 'bg-green-100 text-green-800 border-green-200',
  red: 'bg-red-100 text-red-800 border-red-200',
  teal: 'bg-teal-100 text-teal-800 border-teal-200',
};

export const Badge = ({ status }) => {
  const statusObj = APPLICATION_STATUSES.find(s => s.value === status) || { label: status, color: 'gray' };
  const colorClass = colors[statusObj.color] || colors.gray;

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${colorClass}`}>
      {statusObj.label}
    </span>
  );
};
export default Badge;
