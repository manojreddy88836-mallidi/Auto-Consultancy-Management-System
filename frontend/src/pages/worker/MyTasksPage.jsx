import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ClipboardList, Search, Filter, RefreshCw, Eye, Wrench, CreditCard, Users, ShieldAlert } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getMyTasks } from '../../api/workerTaskApi';

const TYPE_ICONS = { REPAIR: Wrench, COLLECTION: CreditCard, VISIT: Users, RECOVERY: ShieldAlert };
const TYPE_COLORS = {
  REPAIR:'bg-orange-100 text-orange-700', COLLECTION:'bg-green-100 text-green-700',
  VISIT:'bg-blue-100 text-blue-700', RECOVERY:'bg-red-100 text-red-700'
};
const STATUS_COLORS = {
  ASSIGNED:'bg-amber-100 text-amber-700', IN_PROGRESS:'bg-purple-100 text-purple-700',
  COMPLETED:'bg-emerald-100 text-emerald-700', CANCELLED:'bg-gray-100 text-gray-500',
  PAYMENT_COLLECTED:'bg-green-100 text-green-700', BIKE_RECOVERED:'bg-red-100 text-red-700',
  VISITED:'bg-blue-100 text-blue-700', PROMISED_TO_PAY:'bg-indigo-100 text-indigo-700',
  CUSTOMER_UNAVAILABLE:'bg-gray-100 text-gray-600', ESCALATED:'bg-rose-100 text-rose-700',
};
const PRIORITY_DOT = { LOW:'bg-gray-400', NORMAL:'bg-blue-400', HIGH:'bg-orange-400', URGENT:'bg-red-500' };

const MyTasksPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [tasks, setTasks]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch]   = useState('');
  const [typeFilter, setType] = useState(searchParams.get('type') || '');
  const [statusFilter, setStatus] = useState(searchParams.get('status') || '');
  const [total, setTotal]     = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getMyTasks({ type: typeFilter || undefined, status: statusFilter || undefined, size: 100 });
      const data = res.data?.data;
      setTasks(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content?.length ?? 0));
    } catch { toast.error('Failed to load tasks'); }
    finally { setLoading(false); }
  }, [typeFilter, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const filtered = tasks.filter(t => {
    const q = search.toLowerCase();
    return !q || t.title?.toLowerCase().includes(q) || t.customerName?.toLowerCase().includes(q)
              || t.registrationNumber?.toLowerCase().includes(q);
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <ClipboardList className="text-[#1E88E5]" size={24}/> My Tasks
          </h1>
          <p className="text-gray-500 text-sm mt-1">{filtered.length} task{filtered.length !== 1 ? 's' : ''}</p>
        </div>
        <button onClick={load} className="p-2 hover:bg-gray-100 rounded-lg self-start" title="Refresh">
          <RefreshCw size={18} className="text-gray-500"/>
        </button>
      </div>

      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
          <input value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search by title, customer, reg#..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
        </div>
        <div className="flex items-center gap-2">
          <Filter size={15} className="text-gray-400"/>
          <select value={typeFilter} onChange={e => setType(e.target.value)}
            className="px-3 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 outline-none bg-white">
            <option value="">All Types</option>
            <option value="REPAIR">Repair / Service</option>
            <option value="COLLECTION">Payment Collection</option>
            <option value="VISIT">Customer Visit</option>
            <option value="RECOVERY">Bike Recovery</option>
          </select>
          <select value={statusFilter} onChange={e => setStatus(e.target.value)}
            className="px-3 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 outline-none bg-white">
            <option value="">All Statuses</option>
            {['ASSIGNED','IN_PROGRESS','COMPLETED','PAYMENT_COLLECTED','BIKE_RECOVERED','VISITED','PROMISED_TO_PAY','CUSTOMER_UNAVAILABLE','ESCALATED','CANCELLED'].map(s => (
              <option key={s} value={s}>{s.replace(/_/g,' ')}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400 gap-3">
            <ClipboardList size={40} className="opacity-25"/>
            <p className="text-sm font-medium">No tasks found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['Task','Type','Customer','Due','Status','Priority','Action'].map(h => (
                    <th key={h} className="text-left px-5 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map(t => {
                  const Icon = TYPE_ICONS[t.taskType] || ClipboardList;
                  const isOverdue = t.dueDate && new Date(t.dueDate) < new Date() && !['COMPLETED','CANCELLED','PAYMENT_COLLECTED','BIKE_RECOVERED'].includes(t.status);
                  return (
                    <tr key={t.id} className={`hover:bg-gray-50/70 ${isOverdue ? 'bg-red-50/30' : ''}`}>
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-2">
                          <Icon size={15} className="text-gray-400 flex-shrink-0"/>
                          <span className="font-medium text-gray-900 line-clamp-1">{t.title}</span>
                          {isOverdue && <span className="text-xs text-red-500 font-bold">OVERDUE</span>}
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <span className={`px-2 py-1 rounded-full text-xs font-semibold ${TYPE_COLORS[t.taskType]||'bg-gray-100 text-gray-600'}`}>
                          {t.taskType?.replace(/_/g,' ')}
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        <p className="font-medium text-gray-800">{t.customerName || '--'}</p>
                        <p className="text-xs text-gray-400">{t.customerPhone}</p>
                      </td>
                      <td className="px-5 py-4 text-xs text-gray-500">
                        {t.dueDate ? new Date(t.dueDate).toLocaleDateString('en-IN') : '--'}
                      </td>
                      <td className="px-5 py-4">
                        <span className={`px-2 py-1 rounded-full text-xs font-semibold ${STATUS_COLORS[t.status]||'bg-gray-100 text-gray-600'}`}>
                          {t.status?.replace(/_/g,' ')}
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        <span className="flex items-center gap-1.5">
                          <span className={`w-2 h-2 rounded-full ${PRIORITY_DOT[t.priority]||'bg-gray-400'}`}/>
                          <span className="text-xs text-gray-500">{t.priority}</span>
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        <button onClick={() => navigate(`/worker/tasks/${t.id}`)}
                          className="flex items-center gap-1.5 px-3 py-1.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-lg text-xs font-semibold">
                          <Eye size={12}/> Open
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

export default MyTasksPage;