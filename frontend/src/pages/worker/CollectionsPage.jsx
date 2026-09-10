import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { CreditCard, Search, Eye, RefreshCw } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getMyTasks } from '../../api/workerTaskApi';

const STATUS_COLORS = {
  ASSIGNED:'bg-amber-100 text-amber-700', IN_PROGRESS:'bg-purple-100 text-purple-700',
  COMPLETED:'bg-emerald-100 text-emerald-700', PAYMENT_COLLECTED:'bg-green-100 text-green-700',
  BIKE_RECOVERED:'bg-red-100 text-red-700', VISITED:'bg-blue-100 text-blue-700',
  PROMISED_TO_PAY:'bg-indigo-100 text-indigo-700', CUSTOMER_UNAVAILABLE:'bg-gray-100 text-gray-500',
  CANCELLED:'bg-gray-100 text-gray-400', ESCALATED:'bg-rose-100 text-rose-700',
  RECOVERY_ASSIGNED:'bg-orange-100 text-orange-700',
};

const CollectionsPagePage = () => {
  const navigate = useNavigate();
  const [tasks, setTasks]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch]   = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getMyTasks({ type: 'COLLECTION', size: 100 });
      const d = res.data?.data;
      setTasks(d?.content || d || []);
    } catch { toast.error('Failed to load tasks'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = tasks.filter(t => {
    const q = search.toLowerCase();
    return !q || t.title?.toLowerCase().includes(q) || t.customerName?.toLowerCase().includes(q) || t.registrationNumber?.toLowerCase().includes(q);
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <CreditCard className="text-green-600" size={24}/> Payment Collections
          </h1>
          <p className="text-gray-500 text-sm mt-1">{filtered.length} task{filtered.length !== 1 ? 's' : ''}</p>
        </div>
        <button onClick={load} className="p-2 hover:bg-gray-100 rounded-lg self-start"><RefreshCw size={18} className="text-gray-500"/></button>
      </div>
      <div className="relative max-w-sm">
        <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search..."
          className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 outline-none"/>
      </div>
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48"><div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/></div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400 gap-2">
            <CreditCard size={40} className="opacity-20"/>
            <p className="text-sm font-medium">No collection tasks assigned</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['Task','Customer','Phone','Reg #','Due Date','Status','Action'].map(h => (
                    <th key={h} className="text-left px-5 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map(t => (
                  <tr key={t.id} className="hover:bg-gray-50/70">
                    <td className="px-5 py-4 font-medium text-gray-900 max-w-48 truncate">{t.title}</td>
                    <td className="px-5 py-4 text-gray-700">{t.customerName || '--'}</td>
                    <td className="px-5 py-4 text-gray-500 text-xs">{t.customerPhone || '--'}</td>
                    <td className="px-5 py-4 text-gray-500 text-xs font-mono">{t.registrationNumber || '--'}</td>
                    <td className="px-5 py-4 text-xs text-gray-400">{t.dueDate ? new Date(t.dueDate).toLocaleDateString('en-IN') : '--'}</td>
                    <td className="px-5 py-4">
                      <span className={"px-2 py-1 rounded-full text-xs font-semibold " + (STATUS_COLORS[t.status]||'bg-gray-100 text-gray-600')}>
                        {t.status?.replace(/_/g,' ')}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <button onClick={() => navigate("/worker/tasks/" + t.id)}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-lg text-xs font-semibold">
                        <Eye size={12}/> Open
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default CollectionsPagePage;