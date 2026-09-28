import React, { useState, useEffect, useCallback } from 'react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, BarChart, Bar, Legend
} from 'recharts';
import { Users, FileText, CheckCircle, Clock, TrendingUp, AlertTriangle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import useAuth from '../../hooks/useAuth';
import * as adminApi from '../../api/adminApi';
import { getAllApplicationsAdmin } from '../../api/applicationApi';
import * as reportApi from '../../api/reportApi';
import { useNavigate } from 'react-router-dom';

const COLORS = ['#1E88E5','#F59E0B','#10B981','#EF4444','#8B5CF6','#06B6D4','#EC4899'];

const StatCard = ({ title, value, icon, trend, trendUp, color }) => {
  const colors = {
    blue:   'from-blue-500 to-blue-600',
    green:  'from-emerald-500 to-emerald-600',
    yellow: 'from-amber-400 to-amber-500',
    indigo: 'from-indigo-500 to-indigo-600',
  };
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 flex items-start justify-between gap-4 hover:shadow-md transition-shadow">
      <div>
        <p className="text-sm font-medium text-gray-500">{title}</p>
        <p className="text-3xl font-bold text-gray-900 mt-1.5">{value ?? '—'}</p>
        {trend && (
          <p className={`text-xs font-medium mt-2 flex items-center gap-1 ${trendUp ? 'text-emerald-600' : 'text-red-500'}`}>
            <TrendingUp size={12} className={trendUp ? '' : 'rotate-180'} /> {trend} this month
          </p>
        )}
      </div>
      <div className={`p-3 rounded-xl bg-gradient-to-br ${colors[color]} text-white shadow-md`}>
        {icon}
      </div>
    </div>
  );
};

const AdminDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [stats, setStats]           = useState(null);
  const [loading, setLoading]       = useState(true);
  const [recentApps, setRecentApps] = useState([]);
  // Chart data from report APIs (pre-aggregated, no full app fetch)
  const [monthlyData, setMonthlyData] = useState([]);
  const [statusData,  setStatusData]  = useState([]);
  const [mfrData,     setMfrData]     = useState([]);

  const fetchDashboard = useCallback(async (signal) => {
    setLoading(true);
    try {
      const [statsRes, recentRes, monthlyRes, statusRes, mfrRes] = await Promise.allSettled([
        adminApi.getDashboardStats(),
        getAllApplicationsAdmin({ page: 0, size: 5, sort: 'createdAt,desc' }),
        reportApi.getApplicationsByMonth(),
        reportApi.getApplicationsByStatus(),
        reportApi.getBrandStats(),
      ]);

      if (statsRes.status === 'fulfilled') {
        setStats(statsRes.value?.data?.data || statsRes.value?.data);
      }
      if (recentRes.status === 'fulfilled') {
        const d = recentRes.value?.data?.data;
        setRecentApps(Array.isArray(d) ? d : d?.content || []);
      }
      if (monthlyRes.status === 'fulfilled') {
        setMonthlyData(monthlyRes.value?.data?.data || []);
      }
      if (statusRes.status === 'fulfilled') {
        setStatusData(statusRes.value?.data?.data || []);
      }
      if (mfrRes.status === 'fulfilled') {
        setMfrData(mfrRes.value?.data?.data || []);
      }
    } catch (e) {
      if (e?.name !== 'CanceledError' && e?.name !== 'AbortError') {
        toast.error('Failed to load dashboard data');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    fetchDashboard(controller.signal);
    return () => controller.abort();
  }, [fetchDashboard]);

  const statusConfig = {
    DRAFT: 'bg-gray-100 text-gray-600', SUBMITTED: 'bg-blue-100 text-blue-700',
    UNDER_REVIEW: 'bg-yellow-100 text-yellow-700', DOCUMENT_VERIFICATION: 'bg-purple-100 text-purple-700',
    FINANCE_VERIFICATION: 'bg-orange-100 text-orange-700', WORKER_ASSIGNED: 'bg-indigo-100 text-indigo-700',
    APPROVED: 'bg-green-100 text-green-700', REJECTED: 'bg-red-100 text-red-600',
    COMPLETED: 'bg-emerald-100 text-emerald-700',
  };

  return (
    <div className="space-y-7">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard Overview</h1>
        <p className="text-gray-500 text-sm mt-1">Welcome back, {user?.firstName}. Here's what's happening.</p>
      </div>

      {/* Stats Grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {[1,2,3,4].map(i => (
            <div key={i} className="bg-white rounded-2xl border border-gray-100 h-32 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          <StatCard title="Total Customers"    value={stats?.totalCustomers}    icon={<Users size={22}/>}       trend={stats?.newCustomersThisMonth ? `+${stats.newCustomersThisMonth}` : null} trendUp color="blue"   />
          <StatCard title="Total Applications" value={stats?.totalApplications} icon={<FileText size={22}/>}    trend={stats?.newApplicationsThisMonth ? `+${stats.newApplicationsThisMonth} new` : null} trendUp color="indigo" />
          <StatCard title="Pending Review"     value={stats?.pendingApplications}   icon={<Clock size={22}/>}       color="yellow" />
          <StatCard title="Completed"          value={stats?.completedApplications} icon={<CheckCircle size={22}/>} color="green" />
        </div>
      )}

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Applications Trend */}
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between mb-5">
            <div>
              <h2 className="text-base font-bold text-gray-900">Applications Trend</h2>
              <p className="text-xs text-gray-400 mt-0.5">Monthly applications this year</p>
            </div>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={monthlyData} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="blueGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#1E88E5" stopOpacity={0.15}/>
                    <stop offset="95%" stopColor="#1E88E5" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94A3B8' }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94A3B8' }} />
                <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.1)', fontSize: '12px' }} />
                <Area type="monotone" dataKey="count" stroke="#1E88E5" strokeWidth={2.5} fill="url(#blueGrad)" dot={false} activeDot={{ r: 5, fill: '#1E88E5' }} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Status Distribution */}
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
          <div className="mb-5">
            <h2 className="text-base font-bold text-gray-900">Status Distribution</h2>
            <p className="text-xs text-gray-400 mt-0.5">Applications by current status</p>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={statusData.length ? statusData : [{name:'No Data',value:1}]} cx="50%" cy="50%" innerRadius={55} outerRadius={90} paddingAngle={4} dataKey="value">
                  {(statusData.length ? statusData : [{name:'No Data',value:1}]).map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.1)', fontSize: '12px' }} />
                <Legend iconType="circle" iconSize={8} formatter={(v) => <span style={{ fontSize: '11px', color: '#64748B' }}>{v}</span>} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Brand Bar Chart */}
      <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div className="mb-5">
          <h2 className="text-base font-bold text-gray-900">Applications by Brand</h2>
          <p className="text-xs text-gray-400 mt-0.5">Top brands by application volume</p>
        </div>
        <div className="h-56">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={mfrData.length ? mfrData : [{name:'No Data',count:0}]} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
              <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94A3B8' }} />
              <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94A3B8' }} />
              <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.1)', fontSize: '12px' }} />
              <Bar dataKey="count" fill="#1E88E5" radius={[6,6,0,0]} maxBarSize={48}>
                {(mfrData.length ? mfrData : [{name:'No Data',count:0}]).map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Recent Applications */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="text-base font-bold text-gray-900">Recent Applications</h2>
          <button onClick={() => navigate('/admin/applications')} className="text-sm text-[#1E88E5] hover:underline font-medium">
            View all →
          </button>
        </div>
        {recentApps.length === 0 && !loading ? (
          <div className="flex items-center justify-center py-12 text-gray-400">
            <AlertTriangle size={20} className="mr-2 opacity-40" />
            <span className="text-sm">No applications yet</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['App #','Customer','Bike','Finance','Status','Date'].map(h => (
                    <th key={h} className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {recentApps.map(app => (
                  <tr key={app.id} className="hover:bg-gray-50/70 transition-colors cursor-pointer" onClick={() => navigate(`/admin/applications/${app.id}`)}>
                    <td className="px-6 py-4 font-mono text-xs text-gray-500 whitespace-nowrap">{app.applicationNumber}</td>
                    <td className="px-6 py-4">
                      <p className="font-semibold text-gray-900">{app.customerName}</p>
                      <p className="text-xs text-gray-400">{app.customerPhone}</p>
                    </td>
                    <td className="px-6 py-4 text-gray-600 text-xs whitespace-nowrap">
                      {app.manufacturerName} {app.modelName}
                      {app.manufacturingYear && <span className="ml-1 text-gray-400">({app.manufacturingYear})</span>}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${app.underFinance ? 'bg-orange-100 text-orange-700' : 'bg-green-100 text-green-700'}`}>
                        {app.underFinance === true ? 'Finance' : app.underFinance === false ? 'Paid' : '—'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${statusConfig[app.status] || 'bg-gray-100 text-gray-600'}`}>
                        {app.status?.replace(/_/g,' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-gray-400 text-xs whitespace-nowrap">
                      {app.submittedAt ? new Date(app.submittedAt).toLocaleDateString('en-IN') : '—'}
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

export default AdminDashboard;
