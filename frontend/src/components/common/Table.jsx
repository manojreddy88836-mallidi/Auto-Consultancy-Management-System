import React from 'react';
import { ChevronUp, ChevronDown } from 'lucide-react';
import EmptyState from './EmptyState';

export const Table = ({
  columns = [],
  data = [],
  loading = false,
  emptyMessage = 'No data available',
  onSort,
  sortKey,
  sortDir = 'asc'
}) => {
  if (loading) {
    return (
      <div className="w-full border border-gray-200 rounded-lg overflow-hidden bg-white">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              {columns.map((col, idx) => (
                <th key={idx} className="p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {[...Array(5)].map((_, idx) => (
              <tr key={idx} className="border-b border-gray-100 animate-pulse">
                {columns.map((col, cIdx) => (
                  <td key={cIdx} className="p-4">
                    <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden p-8">
        <EmptyState title="No Records Found" description={emptyMessage} />
      </div>
    );
  }

  return (
    <div className="w-full border border-gray-200 rounded-lg overflow-hidden bg-white overflow-x-auto">
      <table className="w-full text-left border-collapse min-w-[800px]">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-200">
            {columns.map((col, idx) => (
              <th 
                key={col.key || idx} 
                className={`p-4 text-xs font-semibold text-gray-500 uppercase tracking-wider ${col.sortable ? 'cursor-pointer hover:bg-gray-100 transition-colors' : ''}`}
                onClick={() => col.sortable && onSort && onSort(col.key)}
              >
                <div className="flex items-center gap-1">
                  {col.label}
                  {col.sortable && sortKey === col.key && (
                    sortDir === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
                  )}
                </div>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, rIdx) => (
            <tr key={row.id || rIdx} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
              {columns.map((col, cIdx) => (
                <td key={col.key || cIdx} className="p-4 text-sm text-gray-700">
                  {col.render ? col.render(row) : row[col.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
export default Table;
