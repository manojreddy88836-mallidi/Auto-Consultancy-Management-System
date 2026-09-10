import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ClipboardList, Search, Filter, RefreshCw, Eye, AlertCircle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getAssignedApplications } from '../../api/applicationApi';

const STATUS_OPTIONS = [
  '','SUBMITTED','UNDER_REVIEW','DOCUMENT_VERIFICATION',
  'FINANCE_VERIFICATION','WORKER_ASSIGNED','APPROVED','REJECTED','COMPLETED'
];
const statusStyle = {
  SUBMITTED:'bg-blue-100 text-blue-700', UNDER_REVIEW:'bg-yellow-100 text-yellow-700',
  DOCUMENT_VERIFICATION:'bg-purple-100 text-purple-700', FINANCE_VERIFICATION:'bg-orange-100 text-orange-700',
  WORKER_ASSIGNED:'bg-indigo-100 text-indigo-700', APPROVED:'bg-green-100 text-green-700',
  REJECTED:'bg-red-100 text-red-600', COMPLETED:'bg-emerald-100 text-emerald-700',
};

const daysDiff = (date) => {
  if (!date) return null;
  return Math.floor((Date.now() - new Date(date)) / 86400000);
};

const AssignedApplicationsPage = () => {
  const navigate = useNavigate();
  const [apps, setApps]           = useState([]);
  const [loading, setLoading]     = useState(true);
  const [search, setSearch]       = useState('');
  const [statusFilter, setFilter] = useState('');

  const fetchApps = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getAssignedApplications();
      setApps(res.data?.data || []);
    } catch { toast.error('Failed to load applications'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchApps(); }, [fetchApps]);

  const filtered = apps.filter(a => {
    const q = search.toLowerCase();
    const matchSearch = !q || a.customerName?.toLowerCase().includes(q)
      || a.applicationNumber?.toLowerCase().includes(q)
      || a.manufacturerName?.toLowerCase().includes(q);
    const matchStatus = !statusFilter || a.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const sorted = [...filtered].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <ClipboardList className="text-[#1E88E5]" size={24}/> Assigned Applications
          </h1>
          <p className="text-gray-500 text-sm mt-1">{filtered.length} application{filtered.length !== 1 ? 's' : ''}</p>
        </div>
        <button onClick={fetchApps} className="p-2 hover:bg-gray-100 rounded-lg self-start" title="Refresh">
          <RefreshCw size={18} className="text-gray-500"/>
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
          <input value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search by customer, app#, bike..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
        </div>
        <div className="flex items-center gap-2">
          <Filter size={15} className="text-gray-400"/>
          <select value={statusFilter} onChange={e => setFilter(e.target.value)}
            className="px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
            <option value="">All Statuses</option>
            {STATUS_OPTIONS.filter(Boolean).map(s => (
              <option key={s} value={s}>{s.replace(/_/g,' ')}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
          </div>
        ) : sorted.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400 gap-3">
            <AlertCircle size={40} className="opacity-25"/>
            <p className="text-sm font-medium">{search || statusFilter ? 'No applications match filters' : 'No applications assigned to you yet'}</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['App #','Customer','Bike','Status','Submitted','Days Open','Action'].map(h => (
                    <th key={h} className="text-left px-5 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {sorted.map(app => {
                  const days = daysDiff(app.submittedAt || app.createdAt);
                  const isUrgent = days !== null && days > 7 && !['APPROVED','REJECTED','COMPLETED'].includes(app.status);
                  return (
                    <tr key={app.id} className={`hover:bg-gray-50/70 transition-colors ${isUrgent ? 'bg-red-50/30' : ''}`}>
                      <td className="px-5 py-4 font-mono text-xs text-gray-500 whitespace-nowrap">{app.applicationNumber}</td>
                      <td className="px-5 py-4">
                        <p className="font-semibold text-gray-900">{app.customerName}</p>
                        <p className="text-xs text-gray-400">{app.customerPhone}</p>
                      </td>
                      <td className="px-5 py-4 text-gray-600 text-xs whitespace-nowrap">
                        {app.manufacturerName} {app.modelName}
                        {app.manufacturingYear && <span className="text-gray-400"> ({app.manufacturingYear})</span>}
                      </td>
                      <td className="px-5 py-4">
                        <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${statusStyle[app.status] || 'bg-gray-100 text-gray-600'}`}>
                          {app.status?.replace(/_/g,' ')}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-xs text-gray-400 whitespace-nowrap">
                        {app.submittedAt ? new Date(app.submittedAt).toLocaleDateString('en-IN') : '—'}
                      </td>
                      <td className="px-5 py-4">
                        {days !== null ? (
                          <span className={`text-xs font-bold ${isUrgent ? 'text-red-500' : 'text-gray-500'}`}>
                            {days}d {isUrgent && '⚠️'}
                          </span>
                        ) : '—'}
                      </td>
                      <td className="px-5 py-4">
                        <button onClick={() => navigate(`/worker/applications/${app.id}`)}
                          className="flex items-center gap-1.5 px-3 py-1.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-lg text-xs font-semibold transition-all">
                          <Eye size={12}/> Review
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default AssignedApplicationsPage;
