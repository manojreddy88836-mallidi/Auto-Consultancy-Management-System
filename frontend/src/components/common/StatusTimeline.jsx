import React from 'react';
import { APPLICATION_STATUSES } from '../../utils/constants';
import { Check } from 'lucide-react';
import { formatDate } from '../../utils/formatters';

export const StatusTimeline = ({ statusHistory = [], currentStatus }) => {
  const currentIndex = APPLICATION_STATUSES.findIndex(s => s.value === currentStatus);
  
  return (
    <div className="py-4">
      <div className="relative border-l-2 border-gray-200 ml-3 md:ml-4 space-y-8">
        {APPLICATION_STATUSES.map((status, index) => {
          const isCompleted = index < currentIndex;
          const isCurrent = index === currentIndex;
          const isFuture = index > currentIndex;
          
          // Find if there's history for this status
          const historyItem = statusHistory.find(h => h.newStatus === status.value);

          return (
            <div key={status.value} className="relative pl-8">
              {/* Circle Marker */}
              <div 
                className={`absolute -left-[9px] top-1 w-4 h-4 rounded-full flex items-center justify-center border-2 
                  ${isCompleted ? 'bg-accent-500 border-accent-500' : 
                    isCurrent ? 'bg-white border-accent-500' : 'bg-white border-gray-300'}`}
              >
                {isCompleted && <Check size={10} className="text-white" />}
                {isCurrent && (
                  <span className="w-2 h-2 rounded-full bg-accent-500 animate-ping absolute"></span>
                )}
                {isCurrent && (
                  <span className="w-2 h-2 rounded-full bg-accent-500 relative"></span>
                )}
              </div>

              {/* Content */}
              <div>
                <h4 className={`text-sm font-semibold ${isFuture ? 'text-gray-400' : 'text-gray-800'}`}>
                  {status.label}
                </h4>
                {historyItem && (
                  <div className="mt-1 text-xs text-gray-500">
                    <p>{historyItem.changedAt ? formatDate(historyItem.changedAt) : 'Date unavailable'}</p>
                    {historyItem.changedByName && <p className="mt-0.5">By: {historyItem.changedByName}</p>}
                    {historyItem.remarks && (
                      <p className="mt-1 bg-gray-50 p-2 rounded text-gray-600 italic inline-block">
                        "{historyItem.remarks}"
                      </p>
                    )}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
export default StatusTimeline;
