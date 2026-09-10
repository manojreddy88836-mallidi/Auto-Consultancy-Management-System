import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, ArrowRight, Info } from 'lucide-react';

/**
 * ManufacturingYears are managed inline on the Bike Models page.
 * This page redirects the admin there with a helpful message.
 */
const ManufacturingYearsPage = () => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center">
      <div className="w-20 h-20 bg-blue-50 rounded-full flex items-center justify-center mb-6">
        <Calendar size={36} className="text-[#1E88E5] opacity-70"/>
      </div>
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Manufacturing Years</h1>
      <p className="text-gray-500 text-sm max-w-md mb-2">
        Manufacturing years are managed directly inside each bike model.
        Expand any model on the <strong>Bike Models</strong> page to add or remove years.
      </p>
      <div className="flex items-center gap-2 text-xs text-blue-600 bg-blue-50 px-4 py-2.5 rounded-xl mt-2 mb-8 border border-blue-100">
        <Info size={14}/>
        Click the <strong>↓ expand arrow</strong> on a model row, then use the Years panel on the right.
      </div>
      <button
        onClick={() => navigate('/admin/bike-models')}
        className="flex items-center gap-2 px-6 py-3 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-full font-semibold transition-all shadow-md hover:shadow-lg hover:scale-[1.02]">
        Go to Bike Models <ArrowRight size={18}/>
      </button>
    </div>
  );
};

export default ManufacturingYearsPage;
