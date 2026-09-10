import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Filter, Bike, ChevronRight, RefreshCw, IndianRupee, Gauge, Calendar, Palette } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getAvailableBikes } from '../../api/bikeInventoryApi';
import { getAllBrands } from '../../api/brandApi';

const BACKEND_BASE = 'http://localhost:8080';

const BikeCard = ({ bike, onViewDetails, onApply }) => {
  const [imgErr, setImgErr] = useState(false);
  const imgUrl = bike.primaryImageUrl ? `${BACKEND_BASE}${bike.primaryImageUrl}` : null;

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-lg transition-all duration-300 group flex flex-col">
      {/* Image */}
      <div className="relative h-52 bg-gradient-to-br from-slate-100 to-slate-200 overflow-hidden">
        {imgUrl && !imgErr ? (
          <img
            src={imgUrl}
            alt={bike.modelName}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            loading="lazy"
            onError={() => setImgErr(true)}
          />
        ) : (
          <div className="absolute inset-0 flex items-center justify-center flex-col text-gray-400">
            <Bike size={48} className="opacity-30 mb-2" />
            <span className="text-sm opacity-50">No image</span>
          </div>
        )}
        <div className="absolute top-3 right-3">
          <span className="bg-emerald-500 text-white text-xs font-bold px-2.5 py-1 rounded-full shadow">
            AVAILABLE
          </span>
        </div>
        {bike.conditionType && (
          <div className="absolute bottom-3 left-3">
            <span className="bg-black/50 text-white text-xs px-2 py-0.5 rounded-full backdrop-blur-sm">
              {bike.conditionType}
            </span>
          </div>
        )}
      </div>

      {/* Details */}
      <div className="p-5 flex flex-col flex-1">
        <p className="text-xs font-semibold text-[#1E88E5] uppercase tracking-wide mb-1">
          {bike.manufacturerName}
        </p>
        <h3 className="text-lg font-bold text-gray-900 mb-1">{bike.modelName}</h3>

        {bike.bikeCode && (
          <p className="text-xs text-gray-400 font-mono mb-2">{bike.bikeCode}</p>
        )}

        <div className="flex flex-wrap gap-1.5 text-xs text-gray-500 mb-3">
          {bike.category   && <span className="bg-gray-100 px-2 py-0.5 rounded-full">{bike.category}</span>}
          {bike.fuelType   && <span className="bg-gray-100 px-2 py-0.5 rounded-full">{bike.fuelType}</span>}
        </div>

        {/* Specs row */}
        <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500 mb-3">
          {bike.year     && <span className="flex items-center gap-1"><Calendar size={11}/>{bike.year}</span>}
          {bike.kmDriven && <span className="flex items-center gap-1"><Gauge size={11}/>{Number(bike.kmDriven).toLocaleString('en-IN')} km</span>}
          {bike.color    && <span className="flex items-center gap-1"><Palette size={11}/>{bike.color}</span>}
        </div>

        {bike.price && (
          <p className="text-xl font-bold text-gray-900 mb-4 flex items-center gap-1">
            <IndianRupee size={16} />
            {Number(bike.price).toLocaleString('en-IN')}
          </p>
        )}

        <div className="flex gap-2 mt-auto pt-3 border-t border-gray-50">
          <button
            onClick={() => onViewDetails(bike.id)}
            className="flex-1 px-3 py-2.5 rounded-xl border border-gray-200 text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
          >
            View Details
          </button>
          <button
            onClick={() => onApply(bike)}
            className="flex-1 px-3 py-2.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white text-sm font-semibold transition-all shadow-md flex items-center justify-center gap-1.5"
          >
            Apply Now <ChevronRight size={14} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default function SaleBikesPage() {
  const navigate = useNavigate();
  const [bikes, setBikes]           = useState([]);
  const [brands, setBrands]         = useState([]);
  const [loading, setLoading]       = useState(true);
  const [search, setSearch]         = useState('');
  const [brandFilter, setBrandFilter] = useState('');

  const fetchBikes = async () => {
    setLoading(true);
    try {
      const params = {};
      if (search.trim())   params.search = search.trim();
      if (brandFilter)     params.manufacturerId = brandFilter;
      // Uses new BikeInventory endpoint â€” only AVAILABLE bikes with images
      const res = await getAvailableBikes(params);
      setBikes(res.data?.data || []);
    } catch { toast.error('Failed to load available bikes'); }
    finally { setLoading(false); }
  };

  useEffect(() => {
    getAllBrands().then(r => setBrands(r.data?.data || [])).catch(() => {});
  }, []);

  useEffect(() => { fetchBikes(); }, [search, brandFilter]);

  const handleApply = (bike) => {
    // Pass bikeInventoryId (the exact physical bike) and bike details for display
    navigate('/customer/submit', {
      state: {
        selectedBike: bike,
        bikeInventoryId: bike.id,
        bikeModelId: bike.bikeModelId,
      }
    });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Bike className="text-[#1E88E5]" size={24} /> Available Bikes
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            {(bikes || []).length} bike{(bikes || []).length !== 1 ? 's' : ''} available for purchase
          </p>
        </div>
        <button onClick={fetchBikes} className="p-2 hover:bg-gray-100 rounded-lg self-start" title="Refresh">
          <RefreshCw size={18} className="text-gray-500" />
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search bikes by model, brand, color..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter size={15} className="text-gray-400 shrink-0" />
          <select
            value={brandFilter}
            onChange={e => setBrandFilter(e.target.value)}
            className="px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white"
          >
            <option value="">All Brands</option>
            {brands.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
        </div>
      </div>

      {/* Grid */}
      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="w-10 h-10 border-4 border-[#1E88E5] border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (bikes || []).length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-gray-400">
          <Bike size={52} className="opacity-20 mb-4" />
          <p className="text-lg font-semibold">No bikes available right now</p>
          <p className="text-sm mt-1">Check back soon for new inventory</p>
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {(bikes || []).map(bike => (
            <BikeCard
              key={bike.id}
              bike={bike}
              onViewDetails={id => navigate(`/customer/bikes/${id}`)}
              onApply={handleApply}
            />
          ))}
        </div>
      )}
    </div>
  );
}
