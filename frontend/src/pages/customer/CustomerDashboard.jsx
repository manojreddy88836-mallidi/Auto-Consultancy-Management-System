import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  FileText, Clock, CheckCircle, XCircle, ArrowRight, Plus,
  AlertCircle, ChevronRight, Bike, Upload, TrendingUp
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import useAuth from '../../hooks/useAuth';
import { getMyApplications } from '../../api/applicationApi';
import { getCustomerDashboard } from '../../api/customerApi';

const statusStyle = {
  DRAFT:                 { cls: 'bg-gray-100 text-gray-600',     label: 'Draft'              },
  SUBMITTED:             { cls: 'bg-blue-100 text-blue-700',     label: 'Submitted'          },
  UNDER_REVIEW:          { cls: 'bg-yellow-100 text-yellow-700', label: 'Under Review'       },
  DOCUMENT_VERIFICATION: { cls: 'bg-purple-100 text-purple-700', label: 'Doc Verification'   },
  FINANCE_VERIFICATION:  { cls: 'bg-orange-100 text-orange-700', label: 'Finance Verification'},
  WORKER_ASSIGNED:       { cls: 'bg-indigo-100 text-indigo-700', label: 'Worker Assigned'    },
  APPROVED:              { cls: 'bg-green-100 text-green-700',   label: 'Approved'           },
  REJECTED:              { cls: 'bg-red-100 text-red-600',       label: 'Rejected'           },
  COMPLETED:             { cls: 'bg-emerald-100 text-emerald-700',label: 'Completed'         },
};

const StatCard = ({ icon: Icon, label, value, color, bg }) => (
  <div className={`${bg} rounded-2xl p-5 border border-current/5 flex flex-col items-center justify-center text-center min-h-[100px]`}>
    <div className={`w-10 h-10 rounded-xl ${color} flex items-center justify-center mb-2 opacity-90`}>
      <Icon size={20} className="text-white"/>
    </div>
    <p className="text-3xl font-bold text-gray-900">{value}</p>
    <p className="text-xs text-gray-500 font-medium mt-0.5">{label}</p>
  </div>
);

const CustomerDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [apps, setApps]         = useState([]);
  const [stats, setStats]       = useState(null);
  const [loading, setLoading]   = useState(true);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [appsRes, statsRes] = await Promise.allSettled([
        getMyApplications(),
        getCustomerDashboard(),
      ]);
      if (appsRes.status === 'fulfilled') {
        const d = appsRes.value.data?.data;
        setApps(Array.isArray(d) ? d : d?.content || []);
      }
      if (statsRes.status === 'fulfilled') {
        setStats(statsRes.value.data?.data || statsRes.value.data);
      }
    } catch { toast.error('Failed to load dashboard'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  // Compute stats from apps if backend dashboard endpoint not available
  const total     = stats?.totalApplications     ?? apps.length;
  const pending   = stats?.pendingApplications   ?? apps.filter(a => ['SUBMITTED','UNDER_REVIEW','DOCUMENT_VERIFICATION','FINANCE_VERIFICATION','WORKER_ASSIGNED'].includes(a.status)).length;
  const approved  = stats?.approvedApplications  ?? apps.filter(a => ['APPROVED','COMPLETED'].includes(a.status)).length;
  const rejected  = stats?.rejectedApplications  ?? apps.filter(a => a.status === 'REJECTED').length;
  const drafts    = apps.filter(a => a.status === 'DRAFT');
  const recent    = [...apps].sort((a,b) => new Date(b.updatedAt||b.createdAt) - new Date(a.updatedAt||a.createdAt)).slice(0,5);

  const firstName = user?.firstName || user?.name?.split(' ')[0] || 'Customer';

  return (
    <div className="max-w-5xl mx-auto space-y-7">

      {/* Welcome */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">
          Welcome back, <span className="text-[#1E88E5]">{firstName}</span>! 👋
        </h1>
        <p className="text-gray-500 mt-1 text-sm">
          Here's a summary of your applications and activity.
        </p>
      </div>

      {/* Draft continuation banner */}
      {drafts.length > 0 && (
        <div className="flex items-center justify-between bg-amber-50 border border-amber-200 rounded-2xl px-5 py-4 gap-4">
          <div className="flex items-center gap-3">
            <AlertCircle size={20} className="text-amber-500 flex-shrink-0"/>
            <div>
              <p className="font-bold text-amber-800 text-sm">
                You have {drafts.length} unfinished application{drafts.length > 1 ? 's' : ''}
              </p>
              <p className="text-xs text-amber-600 mt-0.5">Complete and submit to start the review process.</p>
            </div>
          </div>
          <button
            onClick={() => navigate(`/customer/submit?applicationId=${drafts[0].id}`)}
            className="flex items-center gap-1.5 px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold rounded-xl transition-all whitespace-nowrap flex-shrink-0">
            Continue Draft <ChevronRight size={13}/>
          </button>
        </div>
      )}

      {/* CTA cards row */}
      <div className="grid md:grid-cols-2 gap-4">
        {/* Submit application */}
        <div className="bg-gradient-to-br from-[#0F1B35] via-[#1E3A5F] to-[#1E88E5] rounded-2xl p-6 text-white shadow-xl flex flex-col justify-between gap-4 overflow-hidden relative min-h-[140px]">
          <div className="absolute inset-0 opacity-5"
            style={{ backgroundImage:'radial-gradient(circle at 30% 50%, white 1.5px, transparent 1.5px)', backgroundSize:'28px 28px' }}/>
          <div className="relative z-10">
            <p className="text-white/60 text-xs font-semibold uppercase tracking-widest mb-1">Consultancy Services</p>
            <h2 className="text-xl font-bold mb-1">Need help with your bike?</h2>
            <p className="text-white/70 text-xs">RC transfer, loan NOC, finance verification &amp; more.</p>
          </div>
          <button
            onClick={() => navigate('/customer/submit')}
            className="relative z-10 self-start flex items-center gap-2 px-5 py-2.5 bg-[#F59E0B] hover:bg-[#D97706] text-white rounded-full font-bold text-sm transition-all shadow-lg hover:scale-[1.02]">
            <Plus size={15}/> Submit Application <ArrowRight size={14}/>
          </button>
        </div>

        {/* Browse bikes for sale */}
        <div className="bg-gradient-to-br from-[#1B5E20] to-[#388E3C] rounded-2xl p-6 text-white shadow-xl flex flex-col justify-between gap-4 overflow-hidden relative min-h-[140px]">
          <div className="absolute inset-0 opacity-5"
            style={{ backgroundImage:'radial-gradient(circle at 70% 50%, white 1.5px, transparent 1.5px)', backgroundSize:'28px 28px' }}/>
          <div className="relative z-10">
            <p className="text-white/60 text-xs font-semibold uppercase tracking-widest mb-1">Available Inventory</p>
            <h2 className="text-xl font-bold mb-1">Browse Bikes for Sale</h2>
            <p className="text-white/70 text-xs">View our current inventory and apply for a bike directly.</p>
          </div>
          <button
            onClick={() => navigate('/customer/bikes')}
            className="relative z-10 self-start flex items-center gap-2 px-5 py-2.5 bg-white/20 hover:bg-white/30 text-white rounded-full font-bold text-sm transition-all hover:scale-[1.02]">
            <Bike size={15}/> Browse Bikes <ArrowRight size={14}/>
          </button>
        </div>
      </div>

      {/* Stats */}
      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[1,2,3,4].map(i => <div key={i} className="h-24 bg-white rounded-2xl animate-pulse border border-gray-100"/>)}
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard icon={FileText}    label="Total Applications" value={total}    bg="bg-blue-50"   color="bg-blue-500"   />
          <StatCard icon={Clock}       label="Pending Review"     value={pending}  bg="bg-amber-50"  color="bg-amber-500"  />
          <StatCard icon={CheckCircle} label="Approved"           value={approved} bg="bg-green-50"  color="bg-green-500"  />
          <StatCard icon={XCircle}     label="Rejected"           value={rejected} bg="bg-red-50"    color="bg-red-500"    />
        </div>
      )}

      {/* Recent applications */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="text-base font-bold text-gray-900 flex items-center gap-2">
            <TrendingUp size={16} className="text-[#1E88E5]"/> Recent Applications
          </h2>
          <Link to="/customer/applications" className="text-xs font-semibold text-[#1E88E5] hover:underline flex items-center gap-1">
            View all <ChevronRight size={13}/>
          </Link>
        </div>

        {loading ? (
          <div className="divide-y divide-gray-50">
            {[1,2,3].map(i => <div key={i} className="h-16 bg-white animate-pulse mx-4 my-3 rounded-xl"/>)}
          </div>
        ) : recent.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-14 text-gray-400 gap-3">
            <Bike size={36} className="opacity-20"/>
            <p className="text-sm font-medium">No applications yet</p>
            <button onClick={() => navigate('/customer/submit')}
              className="text-[#1E88E5] text-sm font-semibold hover:underline flex items-center gap-1">
              <Plus size={14}/> Start your first application
            </button>
          </div>
        ) : (
          <div className="divide-y divide-gray-50">
            {recent.map(app => {
              const cfg = statusStyle[app.status] || statusStyle.DRAFT;
              const isDraft = app.status === 'DRAFT';
              return (
                <div key={app.id}
                  className="flex items-center justify-between px-6 py-4 hover:bg-gray-50/70 cursor-pointer transition-colors group"
                  onClick={() => navigate(isDraft ? `/customer/submit?applicationId=${app.id}` : `/customer/applications/${app.id}`)}>
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div className="w-9 h-9 rounded-xl bg-blue-50 flex items-center justify-center flex-shrink-0">
                      <FileText size={15} className="text-blue-400"/>
                    </div>
                    <div className="min-w-0">
                      <p className="font-bold text-sm text-gray-900 truncate">
                        {app.manufacturerName && app.modelName
                          ? `${app.manufacturerName} ${app.modelName}`
                          : app.applicationNumber}
                      </p>
                      <p className="text-xs text-gray-400 font-mono mt-0.5">{app.applicationNumber}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 flex-shrink-0">
                    <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${cfg.cls}`}>
                      {cfg.label}
                    </span>
                    <p className="text-xs text-gray-300 hidden sm:block whitespace-nowrap">
                      {new Date(app.updatedAt || app.createdAt).toLocaleDateString('en-IN')}
                    </p>
                    <ChevronRight size={14} className="text-gray-300 group-hover:text-gray-500 transition-colors"/>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Quick links */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        {[
          { icon: Upload,    label: 'Upload Documents', to: '/customer/documents', color: 'from-blue-500 to-blue-600'     },
          { icon: FileText,  label: 'My Applications',  to: '/customer/applications', color: 'from-indigo-500 to-indigo-600' },
          { icon: Bike,      label: 'My Profile',       to: '/customer/profile',   color: 'from-emerald-500 to-emerald-600' },
        ].map(link => {
          const Icon = link.icon;
          return (
            <Link key={link.to} to={link.to}
              className={`bg-gradient-to-br ${link.color} text-white rounded-2xl p-5 text-center shadow-md hover:shadow-xl hover:scale-[1.02] transition-all`}>
              <Icon size={24} className="mx-auto mb-2 opacity-90"/>
              <p className="text-sm font-bold">{link.label}</p>
            </Link>
          );
        })}
      </div>
    </div>
  );
};

export default CustomerDashboard;
