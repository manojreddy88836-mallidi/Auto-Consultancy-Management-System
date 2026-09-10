import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  CheckCircle, Clock, XCircle, FileText, Bike, CreditCard,
  ArrowLeft, Upload, AlertCircle, ChevronRight
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getApplicationById } from '../../api/applicationApi';
import { getDocuments } from '../../api/documentApi';

const STATUS_ORDER = [
  'SUBMITTED','UNDER_REVIEW','DOCUMENT_VERIFICATION',
  'FINANCE_VERIFICATION','WORKER_ASSIGNED','APPROVED','COMPLETED'
];

const statusStyle = {
  DRAFT:'bg-gray-100 text-gray-600', SUBMITTED:'bg-blue-100 text-blue-700',
  UNDER_REVIEW:'bg-yellow-100 text-yellow-700', DOCUMENT_VERIFICATION:'bg-purple-100 text-purple-700',
  FINANCE_VERIFICATION:'bg-orange-100 text-orange-700', WORKER_ASSIGNED:'bg-indigo-100 text-indigo-700',
  APPROVED:'bg-green-100 text-green-700', REJECTED:'bg-red-100 text-red-600',
  COMPLETED:'bg-emerald-100 text-emerald-700',
};

const statusMessages = {
  DRAFT:                 { title:'Draft',                desc:'Your application is saved as a draft. Complete all steps to submit.' },
  SUBMITTED:             { title:'Application Submitted', desc:'Your application has been received. Our team will review it shortly.' },
  UNDER_REVIEW:          { title:'Under Review',          desc:'Our experts are reviewing your application and bike details.' },
  DOCUMENT_VERIFICATION: { title:'Document Verification', desc:'Please ensure all required documents are uploaded and clear.' },
  FINANCE_VERIFICATION:  { title:'Finance Verification',  desc:'Our finance team is verifying your loan and payment details.' },
  WORKER_ASSIGNED:       { title:'Worker Assigned',       desc:'A dedicated consultant has been assigned to your application.' },
  APPROVED:              { title:'Application Approved! 🎉', desc:'Congratulations! Your application has been approved.' },
  REJECTED:              { title:'Application Rejected',  desc:'Your application was not approved. Please contact support for details.' },
  COMPLETED:             { title:'Process Completed ✅',  desc:'All formalities are complete. Thank you for using our services!' },
};

const InfoRow = ({ label, value }) => (
  <div className="flex justify-between items-center py-2.5 border-b border-gray-50 last:border-0">
    <span className="text-xs text-gray-500 font-medium">{label}</span>
    <span className="text-sm font-semibold text-gray-800">{value ?? '—'}</span>
  </div>
);

const ApplicationStatusPage = () => {
  const { id }   = useParams();
  const navigate = useNavigate();
  const [app, setApp]   = useState(null);
  const [docs, setDocs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      getApplicationById(id),
      getDocuments(id),
    ]).then(([appRes, docsRes]) => {
      setApp(appRes.data?.data || appRes.data);
      setDocs(docsRes.data?.data || []);
    }).catch(() => toast.error('Failed to load application'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
    </div>
  );

  if (!app) return (
    <div className="max-w-2xl mx-auto py-12 text-center text-gray-400">
      <AlertCircle size={40} className="mx-auto mb-3 opacity-30"/>
      <p className="font-medium">Application not found</p>
      <button onClick={() => navigate('/customer/applications')} className="mt-4 text-[#1E88E5] text-sm hover:underline">
        ← Back to my applications
      </button>
    </div>
  );

  const isRejected   = app.status === 'REJECTED';
  const isApproved   = app.status === 'APPROVED' || app.status === 'COMPLETED';
  const currentIdx   = STATUS_ORDER.indexOf(app.status);
  const msg          = statusMessages[app.status] || { title: app.status, desc: '' };
  const docApproved  = docs.filter(d => d.status === 'APPROVED').length;
  const docRejected  = docs.filter(d => d.status === 'REJECTED').length;
  const docPending   = docs.filter(d => d.status === 'UPLOADED' || d.status === 'UNDER_REVIEW').length;

  return (
    <div className="max-w-3xl mx-auto space-y-5">
      {/* Back */}
      <button onClick={() => navigate('/customer/applications')}
        className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 transition-colors">
        <ArrowLeft size={15}/> Back to My Applications
      </button>

      {/* Status Hero */}
      <div className={`rounded-2xl p-6 text-white shadow-xl ${
        isRejected ? 'bg-gradient-to-br from-red-600 to-red-800'
        : isApproved ? 'bg-gradient-to-br from-emerald-500 to-emerald-700'
        : 'bg-gradient-to-br from-[#0F1B35] to-[#1E3A5F]'
      }`}>
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-white/60 text-xs font-medium uppercase tracking-widest mb-2">Application Status</p>
            <h1 className="text-2xl font-bold">{msg.title}</h1>
            <p className="text-white/80 text-sm mt-1.5 max-w-lg">{msg.desc}</p>
          </div>
          {isApproved && <CheckCircle size={48} className="text-white/40 flex-shrink-0"/>}
          {isRejected && <XCircle size={48} className="text-white/40 flex-shrink-0"/>}
          {!isApproved && !isRejected && <Clock size={48} className="text-white/30 flex-shrink-0"/>}
        </div>
        <div className="mt-4 flex flex-wrap gap-3 text-xs">
          <span className="bg-white/10 px-3 py-1.5 rounded-full font-mono">{app.applicationNumber}</span>
          {app.submittedAt && (
            <span className="bg-white/10 px-3 py-1.5 rounded-full">
              Submitted {new Date(app.submittedAt).toLocaleDateString('en-IN')}
            </span>
          )}
        </div>
      </div>

      {/* Progress Tracker */}
      {!isRejected && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
          <h2 className="text-sm font-bold text-gray-900 mb-5 uppercase tracking-wide">Progress Tracker</h2>
          <div className="relative">
            {/* Track line */}
            <div className="absolute left-4 top-4 bottom-4 w-0.5 bg-gray-100"/>
            <div className="space-y-4">
              {STATUS_ORDER.map((s, i) => {
                const isDone   = !isRejected && (isApproved ? true : i < currentIdx);
                const isCurrent = i === currentIdx && !isApproved;
                const isFuture  = i > currentIdx && !isApproved;
                return (
                  <div key={s} className="relative flex items-start gap-4 pl-10">
                    <div className={`absolute left-0 w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 border-2 transition-all ${
                      isDone    ? 'bg-emerald-500 border-emerald-500 shadow-md' :
                      isCurrent ? 'bg-[#1E88E5] border-[#1E88E5] shadow-md animate-pulse' :
                                  'bg-white border-gray-200'
                    }`}>
                      {isDone    ? <CheckCircle size={14} className="text-white"/> :
                       isCurrent ? <Clock size={14} className="text-white"/> :
                                   <span className="text-xs font-bold text-gray-300">{i+1}</span>}
                    </div>
                    <div className={`flex-1 pb-1 ${isFuture ? 'opacity-40' : ''}`}>
                      <p className={`font-semibold text-sm ${isCurrent ? 'text-[#1E88E5]' : isDone ? 'text-gray-900' : 'text-gray-400'}`}>
                        {s.replace(/_/g,' ')}
                      </p>
                      {isCurrent && (
                        <p className="text-xs text-gray-500 mt-0.5">Currently in progress…</p>
                      )}
                      {isDone && (
                        <p className="text-xs text-emerald-600 mt-0.5">Completed ✓</p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Bike + Finance Summary */}
      <div className="grid sm:grid-cols-2 gap-5">
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <h3 className="font-bold text-[#0F1B35] px-5 pt-5 pb-3 flex items-center gap-2 text-sm border-b border-gray-100">
            <Bike size={16}/> Selected Bike
          </h3>
          {/* Bike image */}
          {app.bikeImageUrl && (
            <div className="h-36 bg-gradient-to-br from-slate-100 to-slate-200 overflow-hidden">
              <img
                src={`http://localhost:8080${app.bikeImageUrl}`}
                alt={app.modelName}
                className="w-full h-full object-cover"
              />
            </div>
          )}
          <div className="p-5 space-y-0">
            {/* Price badge */}
            {app.bikePrice && (
              <div className="mb-3 flex items-center gap-1.5 bg-blue-50 border border-blue-100 rounded-xl px-3 py-2">
                <span className="text-blue-700 font-bold text-sm">
                  ₹ {Number(app.bikePrice).toLocaleString('en-IN')}
                </span>
                <span className="text-blue-400 text-xs">Listed Price</span>
                {app.bikeSaleStatus && app.bikeSaleStatus !== 'NOT_FOR_SALE' && (
                  <span className={`ml-auto text-xs font-semibold px-2 py-0.5 rounded-full ${
                    app.bikeSaleStatus === 'AVAILABLE' ? 'bg-green-100 text-green-700' :
                    app.bikeSaleStatus === 'SOLD' ? 'bg-red-100 text-red-700' :
                    'bg-amber-100 text-amber-700'
                  }`}>{app.bikeSaleStatus}</span>
                )}
              </div>
            )}
            <InfoRow label="Brand"        value={app.manufacturerName || app.bikeDetail?.manufacturerName}/>
            <InfoRow label="Model"        value={app.modelName || app.bikeDetail?.modelName}/>
            <InfoRow label="Variant"      value={app.variantName || app.bikeDetail?.variantName}/>
            <InfoRow label="Year"         value={app.manufacturingYear || app.bikeDetail?.manufacturingYear}/>
            <InfoRow label="Reg Number"   value={app.registrationNumber || app.bikeDetail?.registrationNumber}/>
            {app.bikeCode && <InfoRow label="Inventory Code" value={app.bikeCode}/>}
          </div>
        </div>
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <h3 className="font-bold text-[#0F1B35] mb-3 flex items-center gap-2 text-sm"><CreditCard size={16}/> Finance</h3>
          <InfoRow label="Finance Type"   value={app.financeDetail?.underFinance === true ? 'Under Finance' : app.financeDetail?.underFinance === false ? 'No Finance (Paid)' : '—'}/>
          <InfoRow label="Finance Co."    value={app.financeDetail?.financeCompany}/>
          <InfoRow label="Worker"         value={app.workerAssignment?.workerName || 'Not assigned yet'}/>
        </div>
      </div>


      {/* Documents summary */}
      {docs.length > 0 && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold text-[#0F1B35] flex items-center gap-2 text-sm"><FileText size={16}/> Documents ({docs.length})</h3>
            <Link to="/customer/documents" className="text-xs text-[#1E88E5] hover:underline font-medium">Manage →</Link>
          </div>
          <div className="flex flex-wrap gap-3">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-emerald-700 bg-emerald-50 px-3 py-1.5 rounded-full">
              <CheckCircle size={12}/> {docApproved} Approved
            </div>
            {docPending > 0 && (
              <div className="flex items-center gap-1.5 text-xs font-semibold text-yellow-700 bg-yellow-50 px-3 py-1.5 rounded-full">
                <Clock size={12}/> {docPending} Pending Review
              </div>
            )}
            {docRejected > 0 && (
              <div className="flex items-center gap-1.5 text-xs font-semibold text-red-600 bg-red-50 px-3 py-1.5 rounded-full">
                <XCircle size={12}/> {docRejected} Rejected — re-upload needed
              </div>
            )}
          </div>
          {docRejected > 0 && (
            <Link to="/customer/documents"
              className="mt-3 flex items-center gap-2 text-sm font-semibold text-white bg-red-500 hover:bg-red-600 px-4 py-2 rounded-xl w-fit transition-all">
              <Upload size={14}/> Re-upload Rejected Documents <ChevronRight size={14}/>
            </Link>
          )}
        </div>
      )}

      {/* Status History */}
      {app.statusHistory?.length > 0 && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2 text-sm"><Clock size={16}/> Activity Log</h3>
          <div className="relative pl-5">
            <div className="absolute left-1.5 top-0 bottom-0 w-0.5 bg-gray-100"/>
            {app.statusHistory.map((h, i) => (
              <div key={h.id || i} className="relative mb-4 last:mb-0">
                <div className="absolute -left-5 w-3 h-3 rounded-full bg-[#1E88E5] border-2 border-white"/>
                <p className={`text-xs font-bold px-2 py-0.5 rounded-full inline-block mb-1 ${statusStyle[h.newStatus] || 'bg-gray-100 text-gray-600'}`}>
                  {h.newStatus?.replace(/_/g,' ')}
                </p>
                {h.remarks && <p className="text-xs text-gray-600 italic mt-0.5">"{h.remarks}"</p>}
                <p className="text-xs text-gray-300 mt-0.5">
                  {h.changedAt ? new Date(h.changedAt).toLocaleString('en-IN', { dateStyle:'medium', timeStyle:'short' }) : ''}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Help footer */}
      <div className="text-center py-4 text-sm text-gray-500">
        Questions about your application?{' '}
        <Link to="/contact" className="text-[#1E88E5] font-semibold hover:underline">Contact Support →</Link>
      </div>
    </div>
  );
};

export default ApplicationStatusPage;
