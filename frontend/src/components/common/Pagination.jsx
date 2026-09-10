import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export const Pagination = ({
  currentPage = 0,
  totalPages = 1,
  totalElements = 0,
  pageSize = 10,
  onPageChange
}) => {
  if (totalPages <= 1) return null;

  const start = currentPage * pageSize + 1;
  const end = Math.min((currentPage + 1) * pageSize, totalElements);

  // Simple 5-page window logic
  const getPageNumbers = () => {
    let pages = [];
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, currentPage + 2);
    
    if (startPage === 0) endPage = Math.min(totalPages - 1, 4);
    if (endPage === totalPages - 1) startPage = Math.max(0, totalPages - 5);

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  };

  return (
    <div className="flex items-center justify-between px-4 py-3 bg-white border-t border-gray-200 sm:px-6 rounded-b-lg">
      <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
        <div>
          <p className="text-sm text-gray-700">
            Showing <span className="font-medium">{start}</span> to <span className="font-medium">{end}</span> of{' '}
            <span className="font-medium">{totalElements}</span> results
          </p>
        </div>
        <div>
          <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
            <button
              onClick={() => onPageChange(currentPage - 1)}
              disabled={currentPage === 0}
              className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span className="sr-only">Previous</span>
              <ChevronLeft size={16} />
            </button>
            {getPageNumbers().map(pageNum => (
              <button
                key={pageNum}
                onClick={() => onPageChange(pageNum)}
                className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium
                  ${currentPage === pageNum 
                    ? 'z-10 bg-accent-50 border-accent-500 text-accent-600' 
                    : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'}`}
              >
                {pageNum + 1}
              </button>
            ))}
            <button
              onClick={() => onPageChange(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
              className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span className="sr-only">Next</span>
              <ChevronRight size={16} />
            </button>
          </nav>
        </div>
      </div>
    </div>
  );
};
export default Pagination;
