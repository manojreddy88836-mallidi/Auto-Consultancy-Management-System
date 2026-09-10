import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Wrench, CreditCard, Users, ShieldAlert,
  CheckCircle2, Clock, AlertCircle, TrendingUp,
  ClipboardList, RefreshCw
} from 'lucide-react';
import { getTodaySummary } from '../../api/workerTaskApi';
import { toast } from 'react-hot-toast';

// ?? Defined OUTSIDE component to prevent re-creation crash ??
const StatCard = ({ icon: Icon, label, value, color, onClick }) => (
  <div
    onClick={onClick}
    className={`bg-white rounded-2xl p-5 border border-gray-100 shadow-sm flex items-center gap-4 ${onClick ? 'cursor-pointer hover:shadow-md transition-shadow' : ''}`}
  >
    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${color}`}>
      <Icon size={22} className="text-white" />
    </div>
    <div>
      <p className="text-2xl font-bold text-gray-900">{value ?? 0}</p>
      <p className="text-sm text-gray-500">{label}</p>
    </div>
  </div>
);

const WorkerDashboard = () => {
  const navigate = useNavigate();
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const res = await getTodaySummary();
      setSummary(res.data?.data || {});
    } catch {
      toast.error('Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-8 h-8 border-2 border-[#1E88E5] border-t-transparent rounded-full animate-spin" />
    </div>
  );

  const s = summary || {};

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Field Dashboard</h1>
          <p className="text-gray-500 text-sm mt-0.5">
            {new Date().toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          </p>
        </div>
        <button onClick={load} className="p-2 hover:bg-gray-100 rounded-lg" title="Refresh">
          <RefreshCw size={18} className="text-gray-500" />
        </button>
      </div>

      {/* Task Overview */}
      <div>
        <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-3">Task Overview</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard icon={ClipboardList} label="Total Active"  value={(s.assigned || 0) + (s.inProgress || 0)} color="bg-[#1E88E5]" onClick={() => navigate('/worker/tasks')} />
          <StatCard icon={Clock}         label="Pending"       value={s.assigned}     color="bg-amber-500"   onClick={() => navigate('/worker/tasks?status=ASSIGNED')} />
          <StatCard icon={TrendingUp}    label="In Progress"   value={s.inProgress}   color="bg-purple-500"  onClick={() => navigate('/worker/tasks?status=IN_PROGRESS')} />
          <StatCard icon={CheckCircle2}  label="Completed"     value={s.completedToday} color="bg-emerald-500" onClick={() => navigate('/worker/tasks?status=COMPLETED')} />
        </div>
      </div>

      {/* By Task Type */}
      <div>
        <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-3">By Task Type</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard icon={Wrench}      label="Repair / Service"    value={s.repairJobs}      color="bg-orange-500" onClick={() => navigate('/worker/service')} />
          <StatCard icon={CreditCard}  label="Payment Collections" value={s.collectionTasks} color="bg-green-600"  onClick={() => navigate('/worker/collections')} />
          <StatCard icon={Users}       label="Customer Visits"     value={s.visitTasks}      color="bg-blue-600"   onClick={() => navigate('/worker/visits')} />
          <StatCard icon={ShieldAlert} label="Bike Recovery"       value={s.recoveryTasks}   color="bg-red-600"    onClick={() => navigate('/worker/recovery')} />
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
        <h2 className="text-sm font-semibold text-gray-700 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {[
            { label: 'All My Tasks',   path: '/worker/tasks',       color: 'bg-[#1E88E5] text-white' },
            { label: 'Service Jobs',   path: '/worker/service',     color: 'bg-orange-50 text-orange-700 border border-orange-200' },
            { label: 'Collections',    path: '/worker/collections', color: 'bg-green-50 text-green-700 border border-green-200' },
            { label: 'Recovery Tasks', path: '/worker/recovery',    color: 'bg-red-50 text-red-700 border border-red-200' },
          ].map(a => (
            <button key={a.path} onClick={() => navigate(a.path)}
              className={`px-4 py-2.5 rounded-xl text-sm font-semibold transition-all hover:opacity-80 ${a.color}`}>
              {a.label}
            </button>
          ))}
        </div>
      </div>

      {/* Application stats (legacy) */}
      <div className="bg-gray-50 rounded-2xl border border-gray-200 p-5">
        <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-3">Application Verification</h2>
        <div className="flex flex-wrap gap-5 text-sm text-gray-600">
          <span className="flex items-center gap-1.5">
            <AlertCircle size={14} className="text-yellow-500" />
            Assigned: <strong className="ml-1">{s.totalAssigned ?? 0}</strong>
          </span>
          <span className="flex items-center gap-1.5">
            <AlertCircle size={14} className="text-purple-500" />
            Docs to Review: <strong className="ml-1">{s.documentsToReview ?? 0}</strong>
          </span>
        </div>
      </div>
    </div>
  );
};

export default WorkerDashboard;