import React, { useState, useEffect } from 'react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, BarChart, Bar, Legend, RadialBarChart, RadialBar
} from 'recharts';
import { BarChart2, Download, RefreshCw, TrendingUp, CheckCircle, Clock, XCircle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getAllApplicationsAdmin } from '../../api/applicationApi';

const COLORS = ['#1E88E5','#F59E0B','#10B981','#EF4444','#8B5CF6','#06B6D4','#EC4899','#F97316'];

// Build monthly data from real apps array
const buildMonthlyData = (apps) => {
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const counts = Array(12).fill(0);
  apps.forEach(a => {
    if (a.createdAt) {
      const m = new Date(a.createdAt).getMonth();
      counts[m]++;
    }
  });
  return months.map((month, i) => ({ month, count: counts[i] }));
};

const buildStatusData = (apps) => {
  const map = {};
  apps.forEach(a => {
    const label = a.status?.replace(/_/g,' ') || 'Unknown';
    map[label] = (map[label] || 0) + 1;
  });
  return Object.entries(map).map(([name, value]) => ({ name, value }));
};

const buildMfrData = (apps) => {
  const map = {};
  apps.forEach(a => {
    if (a.manufacturerName) map[a.manufacturerName] = (map[a.manufacturerName] || 0) + 1;
  });
  return Object.entries(map)
    .sort((a,b) => b[1]-a[1])
    .slice(0,8)
    .map(([name,count]) => ({ name, count }));
};

const buildFinanceData = (apps) => {
  const financed = apps.filter(a => a.underFinance === true).length;
  const paid     = apps.filter(a => a.underFinance === false).length;
  const pending  = apps.filter(a => a.underFinance === null || a.underFinance === undefined).length;
  return [
    { name:'Under Finance', value: financed },
    { name:'No Finance (Paid)', value: paid },
    { name:'Pending', value: pending },
  ].filter(d => d.value > 0);
};

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-white shadow-xl rounded-xl px-4 py-3 border border-gray-100 text-sm">
      <p className="font-bold text-gray-800">{label}</p>
      {payload.map((p,i) => (
        <p key={i} style={{ color: p.color }} className="font-medium">{p.name}: {p.value}</p>
      ))}
    </div>
  );
};

const ReportsPage = () => {
  const [apps, setApps]     = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchApps = async () => {
    setLoading(true);
    try {
      const res = await getAllApplicationsAdmin({ page: 0, size: 200, sort: 'createdAt,desc' });
      const d = res.data?.data;
      setApps(Array.isArray(d) ? d : d?.content || []);
    } catch {
      toast.error('Failed to load report data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchApps(); }, []);

  const monthly   = buildMonthlyData(apps);
  const statuses  = buildStatusData(apps);
  const mfrs      = buildMfrData(apps);
  const finance   = buildFinanceData(apps);

  const total      = apps.length;
  const approved   = apps.filter(a => a.status === 'APPROVED' || a.status === 'COMPLETED').length;
  const rejected   = apps.filter(a => a.status === 'REJECTED').length;
  const pending    = apps.filter(a => !['APPROVED','REJECTED','COMPLETED','DRAFT'].includes(a.status)).length;

  const handleExport = () => {
    // Build CSV from apps data
    const headers = 'App#,Customer,Brand,Model,Year,Finance,Status,Submitted\n';
    const rows = apps.map(a =>
      `${a.applicationNumber},"${a.customerName}",${a.manufacturerName||''},${a.modelName||''},${a.manufacturingYear||''},${a.underFinance===true?'Financed':a.underFinance===false?'Paid':'—'},${a.status},${a.submittedAt?new Date(a.submittedAt).toLocaleDateString('en-IN'):''}`
    ).join('\n');
    const blob = new Blob([headers + rows], { type: 'text/csv' });
    const url  = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `auto_consultancy_report_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    toast.success('Report exported as CSV');
  };

  if (loading) return (
    <div className="flex items-center justify-center h-80">
      <div className="w-10 h-10 border-4 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
    </div>
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <BarChart2 className="text-[#1E88E5]" size={24}/> Reports & Analytics
          </h1>
          <p className="text-gray-500 text-sm mt-1">Live data — {total} total applications</p>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={fetchApps} className="p-2 hover:bg-gray-100 rounded-lg" title="Refresh data">
            <RefreshCw size={18} className="text-gray-500"/>
          </button>
          <button onClick={handleExport}
            className="flex items-center gap-2 px-4 py-2.5 bg-[#0F1B35] hover:bg-[#1E3A5F] text-white rounded-xl text-sm font-semibold transition-all shadow-md">
            <Download size={16}/> Export CSV
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label:'Total Applications', value: total,    icon: BarChart2,    color:'from-blue-500 to-blue-600'    },
          { label:'Approved / Completed',value: approved, icon: CheckCircle,  color:'from-emerald-500 to-emerald-600'},
          { label:'Pending Processing',  value: pending,  icon: Clock,        color:'from-amber-400 to-amber-500'   },
          { label:'Rejected',            value: rejected, icon: XCircle,      color:'from-red-500 to-red-600'       },
        ].map(s => {
          const Icon = s.icon;
          return (
            <div key={s.label} className={`bg-gradient-to-br ${s.color} text-white rounded-2xl p-5 shadow-md`}>
              <div className="flex items-center justify-between mb-2">
                <Icon size={22} className="opacity-80"/>
                <TrendingUp size={14} className="opacity-60"/>
              </div>
              <p className="text-3xl font-bold">{s.value}</p>
              <p className="text-xs font-medium text-white/80 mt-1">{s.label}</p>
            </div>
          );
        })}
      </div>

      {/* Monthly trend + Status pie */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
          <div className="mb-5">
            <h2 className="text-base font-bold text-gray-900">Monthly Applications (Current Year)</h2>
            <p className="text-xs text-gray-400 mt-0.5">Total applications submitted per month</p>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={monthly} margin={{ top:5, right:10, left:-20, bottom:0 }}>
                <defs>
                  <linearGradient id="repGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#1E88E5" stopOpacity={0.15}/>
                    <stop offset="95%" stopColor="#1E88E5" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9"/>
                <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fontSize:11, fill:'#94A3B8' }}/>
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize:11, fill:'#94A3B8' }} allowDecimals={false}/>
                <Tooltip content={<CustomTooltip/>}/>
                <Area type="monotone" dataKey="count" name="Applications" stroke="#1E88E5" strokeWidth={2.5}
                  fill="url(#repGrad)" dot={false} activeDot={{ r:5, fill:'#1E88E5' }}/>
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
          <div className="mb-5">
            <h2 className="text-base font-bold text-gray-900">Status Distribution</h2>
            <p className="text-xs text-gray-400 mt-0.5">Breakdown by current application status</p>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={statuses} cx="50%" cy="50%" innerRadius={55} outerRadius={90}
                  paddingAngle={3} dataKey="value">
                  {statuses.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]}/>)}
                </Pie>
                <Tooltip content={<CustomTooltip/>}/>
                <Legend iconType="circle" iconSize={8}
                  formatter={v => <span style={{ fontSize:'11px', color:'#64748B' }}>{v}</span>}/>
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Brand bar + Finance pie */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
          <div className="mb-5">
            <h2 className="text-base font-bold text-gray-900">Applications by Brand</h2>
            <p className="text-xs text-gray-400 mt-0.5">Top brands by volume</p>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={mfrs} margin={{ top:5, right:10, left:-20, bottom:0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9"/>
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize:11, fill:'#94A3B8' }}/>
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize:11, fill:'#94A3B8' }} allowDecimals={false}/>
                <Tooltip content={<CustomTooltip/>}/>
                <Bar dataKey="count" name="Applications" radius={[6,6,0,0]} maxBarSize={48}>
                  {mfrs.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]}/>)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
          <div className="mb-5">
            <h2 className="text-base font-bold text-gray-900">Finance Split</h2>
            <p className="text-xs text-gray-400 mt-0.5">Financed vs paid outright</p>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={finance} cx="50%" cy="50%" outerRadius={90} paddingAngle={3} dataKey="value">
                  {finance.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]}/>)}
                </Pie>
                <Tooltip content={<CustomTooltip/>}/>
                <Legend iconType="circle" iconSize={8}
                  formatter={v => <span style={{ fontSize:'10px', color:'#64748B' }}>{v}</span>}/>
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Approval rate card */}
      {total > 0 && (
        <div className="bg-gradient-to-br from-[#0F1B35] to-[#1E3A5F] rounded-2xl p-6 text-white shadow-xl">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-[#F59E0B]">{((approved/total)*100).toFixed(1)}%</p>
              <p className="text-xs text-white/70 mt-1 font-medium">Approval Rate</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-red-400">{((rejected/total)*100).toFixed(1)}%</p>
              <p className="text-xs text-white/70 mt-1 font-medium">Rejection Rate</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-blue-300">{((pending/total)*100).toFixed(1)}%</p>
              <p className="text-xs text-white/70 mt-1 font-medium">In Progress</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-emerald-400">
                {apps.filter(a=>a.underFinance===true).length > 0
                  ? `${((apps.filter(a=>a.underFinance===true).length/total)*100).toFixed(1)}%`
                  : '—'}
              </p>
              <p className="text-xs text-white/70 mt-1 font-medium">Finance Rate</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReportsPage;
