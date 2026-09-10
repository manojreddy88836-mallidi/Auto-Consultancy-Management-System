import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Plus, FileText, ChevronRight, AlertCircle, Clock, CheckCircle,
  XCircle, Edit3, Eye, Bike, IndianRupee
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getMyApplications } from '../../api/applicationApi';

const BACKEND_BASE = 'http://localhost:8080';

const statusStyle = {
  DRAFT:                 { cls: 'bg-gray-100 text-gray-600',     icon: Edit3,       label: 'Draft'                },
  SUBMITTED:             { cls: 'bg-blue-100 text-blue-700',     icon: Clock,       label: 'Submitted'            },
  UNDER_REVIEW:          { cls: 'bg-yellow-100 text-yellow-700', icon: Clock,       label: 'Under Review'         },
  DOCUMENT_VERIFICATION: { cls: 'bg-purple-100 text-purple-700', icon: FileText,    label: 'Doc Verification'     },
  FINANCE_VERIFICATION:  { cls: 'bg-orange-100 text-orange-700', icon: Clock,       label: 'Finance Verification' },
  WORKER_ASSIGNED:       { cls: 'bg-indigo-100 text-indigo-700', icon: Clock,       label: 'Worker Assigned'      },
  APPROVED:              { cls: 'bg-green-100 text-green-700',   icon: CheckCircle, label: 'Approved'             },
  REJECTED:              { cls: 'bg-red-100 text-red-600',       icon: XCircle,     label: 'Rejected'             },
  COMPLETED:             { cls: 'bg-emerald-100 text-emerald-700',icon: CheckCircle,label: 'Completed'            },
};

const MyApplicationsPage = () => {
  const navigate = useNavigate();
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchApps = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getMyApplications();
      const d = res.data?.data;
      setApplications(Array.isArray(d) ? d : d?.content || []);
    } catch { toast.error('Failed to load applications'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchApps(); }, [fetchApps]);

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
    </div>
  );

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Applications</h1>
          <p className="text-gray-500 text-sm mt-1">{(applications || []).length} application{(applications || []).length !== 1 ? 's' : ''} total</p>
        </div>
        <button onClick={() => navigate('/customer/bikes')}
          className="flex items-center gap-2 px-5 py-2.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-full font-semibold text-sm transition-all shadow-md hover:shadow-lg hover:scale-[1.02]">
          <Bike size={18}/> Browse Bikes to Apply
        </button>
      </div>

      {/* Empty state */}
      {(applications || []).length === 0 ? (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-16 flex flex-col items-center text-center">
          <div className="w-20 h-20 bg-blue-50 rounded-full flex items-center justify-center mb-5">
            <FileText size={36} className="text-[#1E88E5] opacity-60"/>
          </div>
          <h3 className="text-lg font-bold text-gray-900 mb-2">No applications yet</h3>
          <p className="text-gray-500 text-sm mb-6 max-w-xs">
            Browse available resale bikes and click "Apply for This Bike" to get started.
          </p>
          <button onClick={() => navigate('/customer/bikes')}
            className="flex items-center gap-2 px-6 py-3 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-full font-semibold transition-all shadow-md hover:shadow-lg">
            <Bike size={18}/> Browse Available Bikes
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {(applications || []).map(app => {
            const cfg = statusStyle[app.status] || statusStyle.DRAFT;
            const StatusIcon = cfg.icon;
            const isDraft = app.status === 'DRAFT';
            const hasImage = !!app.bikeImageUrl;
            return (
              <div key={app.id}
                className="bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-all cursor-pointer group overflow-hidden"
                onClick={() => navigate(isDraft ? `/customer/submit?applicationId=${app.id}` : `/customer/applications/${app.id}`)}>
                <div className="flex">
                  {/* Bike thumbnail */}
                  <div className="w-28 flex-shrink-0 bg-gradient-to-br from-slate-100 to-slate-200 relative">
                    {hasImage ? (
                      <img
                        src={`${BACKEND_BASE}${app.bikeImageUrl}`}
                        alt={app.modelName}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="absolute inset-0 flex items-center justify-center">
                        <Bike size={28} className="text-gray-300"/>
                      </div>
                    )}
                  </div>

                  {/* Content */}
                  <div className="flex-1 p-5 min-w-0">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1 min-w-0">
                        <div className="flex flex-wrap items-center gap-2 mb-1">
                          <span className="font-mono text-xs text-gray-400">{app.applicationNumber}</span>
                          <span className={`flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold ${cfg.cls}`}>
                            <StatusIcon size={10}/> {cfg.label}
                          </span>
                        </div>

                        {/* Bike info from flat DTO fields */}
                        <p className="font-bold text-gray-900 text-sm">
                          {app.manufacturerName && app.modelName
                            ? `${app.manufacturerName} ${app.modelName}${app.manufacturingYear ? ` (${app.manufacturingYear})` : ''}`
                            : 'Bike details not submitted'}
                        </p>

                        {app.registrationNumber && (
                          <p className="text-xs text-gray-400 mt-0.5 font-mono">{app.registrationNumber}</p>
                        )}

                        {/* Price + finance info */}
                        <div className="flex flex-wrap gap-2 mt-2">
                          {app.bikePrice && (
                            <span className="flex items-center gap-1 text-xs font-semibold bg-blue-50 text-blue-700 px-2.5 py-0.5 rounded-full">
                              <IndianRupee size={10}/> {Number(app.bikePrice).toLocaleString('en-IN')} Listed
                            </span>
                          )}
                          {app.bikeCode && (
                            <span className="text-xs bg-gray-50 text-gray-500 px-2.5 py-0.5 rounded-full font-mono">{app.bikeCode}</span>
                          )}
                          {app.underFinance !== null && app.underFinance !== undefined && (
                            <span className={`text-xs px-2 py-0.5 rounded font-medium ${
                              app.underFinance ? 'bg-orange-50 text-orange-600' : 'bg-green-50 text-green-600'
                            }`}>
                              {app.underFinance ? 'ðŸ’³ Under Finance' : 'âœ… No Finance'}
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Right: date + action */}
                      <div className="flex flex-col items-end gap-2 flex-shrink-0">
                        <p className="text-xs text-gray-400">
                          {app.submittedAt
                            ? `Submitted ${new Date(app.submittedAt).toLocaleDateString('en-IN')}`
                            : `Created ${new Date(app.createdAt).toLocaleDateString('en-IN')}`}
                        </p>
                        <button className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                          isDraft
                            ? 'bg-[#F59E0B] hover:bg-[#D97706] text-white'
                            : 'bg-gray-100 group-hover:bg-[#1E88E5] group-hover:text-white text-gray-600'
                        }`}>
                          {isDraft ? <><Edit3 size={12}/> Continue</> : <><Eye size={12}/> Track Status</>}
                          <ChevronRight size={12}/>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default MyApplicationsPage;
