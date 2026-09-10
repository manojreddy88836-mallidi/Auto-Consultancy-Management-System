import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import {
  ArrowLeft, Bike, CreditCard, FileText, Clock, User,
  CheckCircle, XCircle, AlertCircle, ChevronRight, Download
} from 'lucide-react';
import { getApplicationById, updateApplicationStatus, assignWorker } from '../../api/applicationApi';
import { getDocuments } from '../../api/documentApi';
import { getWorkers } from '../../api/adminApi';

const BACKEND_BASE = 'http://localhost:8080';
const saleBadge = (s) => ({
  AVAILABLE: 'bg-green-100 text-green-700',
  RESERVED:  'bg-amber-100 text-amber-700',
  SOLD:      'bg-red-100 text-red-600',
})[s] || 'bg-gray-100 text-gray-500';

const ALL_STATUSES = [
  'SUBMITTED','UNDER_REVIEW','DOCUMENT_VERIFICATION',
  'FINANCE_VERIFICATION','WORKER_ASSIGNED','APPROVED','REJECTED','COMPLETED'
];

const statusStyle = {
  DRAFT:'bg-gray-100 text-gray-600',SUBMITTED:'bg-blue-100 text-blue-700',
  UNDER_REVIEW:'bg-yellow-100 text-yellow-700',DOCUMENT_VERIFICATION:'bg-purple-100 text-purple-700',
  FINANCE_VERIFICATION:'bg-orange-100 text-orange-700',WORKER_ASSIGNED:'bg-indigo-100 text-indigo-700',
  APPROVED:'bg-green-100 text-green-700',REJECTED:'bg-red-100 text-red-600',
  COMPLETED:'bg-emerald-100 text-emerald-700',
};

const docStatusStyle = {
  UPLOADED:'bg-blue-100 text-blue-700', UNDER_REVIEW:'bg-yellow-100 text-yellow-700',
  APPROVED:'bg-green-100 text-green-700', REJECTED:'bg-red-100 text-red-600',
};

const InfoRow = ({ label, value }) => (
  <div className="flex justify-between items-start py-2.5 border-b border-gray-50 last:border-0">
    <span className="text-xs text-gray-500 font-medium uppercase tracking-wide">{label}</span>
    <span className="text-sm font-semibold text-gray-800 text-right max-w-[55%]">{value ?? '—'}</span>
  </div>
);

const TABS = [
  { id:'overview', label:'Overview',        icon: User },
  { id:'bike',     label:'Bike Details',    icon: Bike },
  { id:'finance',  label:'Finance',         icon: CreditCard },
  { id:'docs',     label:'Documents',       icon: FileText },
  { id:'history',  label:'Status History',  icon: Clock },
];

const ApplicationDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [app, setApp]               = useState(null);
  const [docs, setDocs]             = useState([]);
  const [workers, setWorkers]       = useState([]);
  const [loading, setLoading]       = useState(true);
  const [activeTab, setActiveTab]   = useState('overview');
  const [newStatus, setNewStatus]   = useState('');
  const [remarks, setRemarks]       = useState('');
  const [selWorker, setSelWorker]   = useState('');
  const [saving, setSaving]         = useState(false);

  useEffect(() => {
    Promise.allSettled([
      getApplicationById(id),
      getDocuments(id),
      getWorkers(),
    ]).then(([appR, docsR, workR]) => {
      if (appR.status === 'fulfilled') {
        const d = appR.value?.data?.data || appR.value?.data;
        setApp(d);
        setNewStatus(d?.status || '');
      } else toast.error('Failed to load application');
      if (docsR.status === 'fulfilled') setDocs(docsR.value?.data?.data || []);
      if (workR.status === 'fulfilled') {
        const wd = workR.value?.data?.data;
        setWorkers(Array.isArray(wd) ? wd : wd?.content || []);
      }
    }).finally(() => setLoading(false));
  }, [id]);

  const handleUpdateStatus = async () => {
    if (!newStatus) { toast.error('Select a status'); return; }
    setSaving(true);
    try {
      await updateApplicationStatus(id, { status: newStatus, remarks });
      toast.success('Status updated');
      setRemarks('');
      const res = await getApplicationById(id);
      setApp(res.data?.data || res.data);
    } catch (e) { toast.error(e?.response?.data?.message || 'Update failed'); }
    finally { setSaving(false); }
  };

  const handleAssign = async () => {
    if (!selWorker) { toast.error('Select a worker'); return; }
    setSaving(true);
    try {
      await assignWorker(id, selWorker);
      toast.success('Worker assigned');
      const res = await getApplicationById(id);
      setApp(res.data?.data || res.data);
    } catch (e) { toast.error(e?.response?.data?.message || 'Assignment failed'); }
    finally { setSaving(false); }
  };

  if (loading) return (
    <div className="flex items-center justify-center h-80">
      <div className="w-10 h-10 border-4 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
    </div>
  );
  if (!app) return (
    <div className="flex flex-col items-center justify-center h-80 text-gray-400 gap-3">
      <AlertCircle size={40} className="opacity-30"/>
      <p className="font-medium">Application not found</p>
      <button onClick={() => navigate('/admin/applications')} className="text-[#1E88E5] text-sm hover:underline">← Back to list</button>
    </div>
  );

  const bd = app.bikeDetail;
  const fd = app.financeDetail;
  const history = app.statusHistory || [];

  return (
    <div className="space-y-6 max-w-5xl">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
        <div>
          <button onClick={() => navigate('/admin/applications')}
            className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 mb-3 transition-colors">
            <ArrowLeft size={15}/> Back to Applications
          </button>
          <h1 className="text-2xl font-bold text-gray-900">{app.applicationNumber}</h1>
          <div className="flex items-center gap-3 mt-2">
            <span className={`px-3 py-1 rounded-full text-xs font-bold ${statusStyle[app.status] || 'bg-gray-100 text-gray-600'}`}>
              {app.status?.replace(/_/g,' ')}
            </span>
            {app.submittedAt && <span className="text-xs text-gray-400">Submitted {new Date(app.submittedAt).toLocaleDateString('en-IN')}</span>}
          </div>
        </div>

        {/* Quick actions */}
        <div className="flex flex-col gap-3 min-w-[240px]">
          <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm space-y-3">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Update Status</p>
            <select value={newStatus} onChange={e => setNewStatus(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
              {ALL_STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g,' ')}</option>)}
            </select>
            <textarea rows={2} value={remarks} onChange={e => setRemarks(e.target.value)}
              placeholder="Add remarks (optional)" className="w-full px-3 py-2 rounded-lg border border-gray-200 text-xs resize-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
            <button onClick={handleUpdateStatus} disabled={saving}
              className="w-full py-2 bg-[#0F1B35] hover:bg-[#1E3A5F] text-white rounded-lg text-sm font-semibold transition-all disabled:opacity-60">
              {saving ? 'Updating…' : 'Update Status'}
            </button>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm space-y-3">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Assign Worker</p>
            <select value={selWorker} onChange={e => setSelWorker(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
              <option value="">Select worker…</option>
              {workers.map(w => <option key={w.id} value={w.id}>{w.user?.firstName} {w.user?.lastName}</option>)}
            </select>
            <button onClick={handleAssign} disabled={saving || !selWorker}
              className="w-full py-2 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-lg text-sm font-semibold transition-all disabled:opacity-60">
              Assign
            </button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="flex border-b border-gray-100 overflow-x-auto">
          {TABS.map(tab => {
            const Icon = tab.icon;
            return (
              <button key={tab.id} onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-5 py-4 text-sm font-medium whitespace-nowrap border-b-2 transition-colors ${
                  activeTab === tab.id ? 'border-[#1E88E5] text-[#1E88E5]' : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}>
                <Icon size={15}/>{tab.label}
              </button>
            );
          })}
        </div>

        <div className="p-6">
          {/* Overview Tab */}
          {activeTab === 'overview' && (
            <div className="grid md:grid-cols-2 gap-6">
              <div className="bg-gray-50 rounded-xl p-5">
                <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><User size={16}/> Customer</h3>
                <InfoRow label="Name"   value={app.customerName  || app.customer?.fullName}/>
                <InfoRow label="Email"  value={app.customerEmail || app.customer?.email}/>
                <InfoRow label="Phone"  value={app.customerPhone || app.customer?.phone}/>
                <InfoRow label="City"   value={app.customer?.city}/>
                <InfoRow label="State"  value={app.customer?.state}/>
              </div>
              <div className="bg-gray-50 rounded-xl p-5">
                <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><Bike size={16}/> Bike Summary</h3>
                {/* Bike image if available from sale inventory */}
                {app.bikeImageUrl && (
                  <div className="mb-4 rounded-xl overflow-hidden aspect-[16/9] max-h-40 bg-gray-200">
                    <img src={`${BACKEND_BASE}${app.bikeImageUrl}`} alt="Bike" className="w-full h-full object-cover" />
                  </div>
                )}
                {app.bikeSaleStatus && app.bikeSaleStatus !== 'NOT_FOR_SALE' && (
                  <div className="mb-3 flex items-center gap-2">
                    <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${saleBadge(app.bikeSaleStatus)}`}>
                      {app.bikeSaleStatus}
                    </span>
                    {app.bikePrice && (
                      <span className="text-sm font-bold text-gray-800">
                        ₹ {Number(app.bikePrice).toLocaleString('en-IN')}
                        <span className="text-xs text-gray-400 font-normal ml-1">Listed</span>
                      </span>
                    )}
                  </div>
                )}
                <InfoRow label="Brand"        value={app.manufacturerName || app.bikeDetail?.manufacturerName}/>
                <InfoRow label="Model"        value={app.modelName || app.bikeDetail?.modelName}/>
                <InfoRow label="Variant"      value={app.variantName || app.bikeDetail?.variantName}/>
                <InfoRow label="Year"         value={app.manufacturingYear || app.bikeDetail?.manufacturingYear}/>
                <InfoRow label="Reg No."      value={app.registrationNumber || app.bikeDetail?.registrationNumber}/>
                {app.bikeCode && <InfoRow label="Inventory Code" value={app.bikeCode}/>}
                {app.bikeInventoryId && <InfoRow label="Inventory ID" value={`#${app.bikeInventoryId}`}/>}
              </div>
              <div className="bg-gray-50 rounded-xl p-5">
                <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><CreditCard size={16}/> Finance Summary</h3>
                <InfoRow label="Under Finance" value={app.financeDetail?.underFinance === true ? 'Yes' : app.financeDetail?.underFinance === false ? 'No' : '—'}/>
                <InfoRow label="Finance Co"    value={app.financeDetail?.financeCompany}/>
                <InfoRow label="Finance Status" value={app.financeDetail?.financeStatus}/>
              </div>
              <div className="bg-gray-50 rounded-xl p-5">
                <h3 className="font-bold text-[#0F1B35] mb-4">Assignment</h3>
                <InfoRow label="Assigned Worker" value={app.workerAssignment?.workerName || 'Unassigned'}/>
                <InfoRow label="Assigned At"     value={app.workerAssignment?.assignedAt ? new Date(app.workerAssignment.assignedAt).toLocaleString('en-IN') : '—'}/>
                <InfoRow label="Created At"      value={app.createdAt ? new Date(app.createdAt).toLocaleString('en-IN') : '—'}/>
                <InfoRow label="Updated At"      value={app.updatedAt ? new Date(app.updatedAt).toLocaleString('en-IN') : '—'}/>
              </div>
            </div>
          )}

          {/* Bike Tab */}
          {activeTab === 'bike' && bd ? (
            <div className="max-w-2xl">
              {/* Bike image if from sale inventory */}
              {app.bikeImageUrl && (
                <div className="mb-5 rounded-2xl overflow-hidden aspect-[16/9] max-h-56 bg-gray-100 border border-gray-200">
                  <img src={`${BACKEND_BASE}${app.bikeImageUrl}`} alt="Bike" className="w-full h-full object-cover" />
                </div>
              )}
              {app.bikeSaleStatus && app.bikeSaleStatus !== 'NOT_FOR_SALE' && (
                <div className="mb-4 flex items-center gap-2">
                  <span className={`px-3 py-1 rounded-full text-xs font-bold ${saleBadge(app.bikeSaleStatus)}`}>
                    Sale Status: {app.bikeSaleStatus}
                  </span>
                  {app.bikePrice && (
                    <span className="text-sm font-semibold text-gray-700">₹ {Number(app.bikePrice).toLocaleString('en-IN')}</span>
                  )}
                </div>
              )}
              <div className="bg-gray-50 rounded-xl p-5 space-y-0">
                <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><Bike size={16}/> Bike Details</h3>
                {(app.bikeCode || app.bikeInventoryId) && (
                  <div className="mb-3 flex items-center gap-2 flex-wrap">
                    {app.bikeInventoryId && (
                      <span className="text-xs text-gray-500 font-medium">Inventory ID: <span className="font-bold text-gray-800">#{app.bikeInventoryId}</span></span>
                    )}
                    {app.bikeCode && (
                      <span className="ml-auto font-mono text-sm font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded">{app.bikeCode}</span>
                    )}
                  </div>
                )}
                <InfoRow label="Brand"        value={bd.manufacturerName}/>
                <InfoRow label="Model"           value={bd.modelName}/>
                <InfoRow label="Variant"         value={bd.variantName}/>
                <InfoRow label="Year"            value={bd.manufacturingYear}/>
                <InfoRow label="Reg Number"      value={bd.registrationNumber}/>
                <InfoRow label="Colour"          value={bd.colour}/>
                <InfoRow label="Purchase Date"   value={bd.purchaseDate}/>
              </div>
            </div>
          ) : activeTab === 'bike' && (
            <div className="flex items-center justify-center py-12 text-gray-400 gap-2">
              <AlertCircle size={18} className="opacity-50"/><span className="text-sm">No bike details submitted yet</span>
            </div>
          )}

          {/* Finance Tab */}
          {activeTab === 'finance' && fd ? (
            <div className="max-w-2xl bg-gray-50 rounded-xl p-5 space-y-0">
              <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><CreditCard size={16}/> Finance Details</h3>
              <InfoRow label="Under Finance"       value={fd.underFinance ? 'Yes' : 'No'}/>
              {fd.underFinance && <>
                {fd.emiAmount && (
                  <div className="my-3 grid grid-cols-2 md:grid-cols-4 gap-2">
                    {[
                      { label: 'Monthly EMI',    value: `₹${Number(fd.emiAmount).toLocaleString('en-IN', {minimumFractionDigits:2})}`,    cls: 'bg-[#1E88E5] text-white' },
                      { label: 'Total Payable',  value: fd.totalPayable  ? `₹${Number(fd.totalPayable).toLocaleString('en-IN', {minimumFractionDigits:2})}` : '—', cls: 'bg-gray-50' },
                      { label: 'Total Interest', value: fd.totalInterest ? `₹${Number(fd.totalInterest).toLocaleString('en-IN', {minimumFractionDigits:2})}` : '—', cls: 'bg-orange-50 text-orange-700' },
                      { label: 'No. of Months',  value: fd.numberOfEmis  ? `${fd.numberOfEmis} months` : '—',   cls: 'bg-gray-50' },
                    ].map(c => (
                      <div key={c.label} className={`rounded-xl p-3 text-center border border-gray-100 ${c.cls}`}>
                        <p className="text-xs font-semibold mb-0.5 opacity-70">{c.label}</p>
                        <p className="text-sm font-bold">{c.value}</p>
                      </div>
                    ))}
                  </div>
                )}
                <InfoRow label="Loan Amount"       value={fd.loanAmount ? `₹${Number(fd.loanAmount).toLocaleString('en-IN')}` : '—'}/>
                <InfoRow label="Annual Rate"       value={fd.annualInterestRate != null ? `${fd.annualInterestRate}% p.a.` : '—'}/>
                <InfoRow label="Tenure"            value={
                  fd.tenureMonths
                    ? `${fd.tenureMonths} months`
                    : fd.tenureYears
                    ? `${fd.tenureYears * 12} months`
                    : '-'
                }/>
                <InfoRow label="Finance Company"   value={fd.financeCompany}/>
                <InfoRow label="Loan Account"      value={fd.loanAccountNumber}/>
                <InfoRow label="Loan Start Date"   value={fd.loanStartDate}/>
                <InfoRow label="Paid EMIs"         value={fd.paidEmis}/>
                <InfoRow label="Remaining EMIs"    value={fd.remainingEmis}/>
                <InfoRow label="Outstanding"       value={fd.outstandingLoanAmount ? `₹${Number(fd.outstandingLoanAmount).toLocaleString('en-IN')}` : '—'}/>
                <InfoRow label="Next EMI Due"      value={fd.nextEmiDueDate}/>
                <InfoRow label="Loan Status"       value={fd.loanClosureStatus}/>
                <InfoRow label="NOC Available"     value={fd.nocAvailable ? 'Yes' : 'No'}/>
              </>}
              {!fd.underFinance && <>
                <InfoRow label="Payment Method"    value={fd.purchasePaymentMethod}/>
                <InfoRow label="Bike Paid"         value={fd.bikePaid ? 'Yes' : 'No'}/>
              </>}
            </div>
          ) : activeTab === 'finance' && (
            <div className="flex items-center justify-center py-12 text-gray-400 gap-2">
              <AlertCircle size={18} className="opacity-50"/><span className="text-sm">No finance details submitted yet</span>
            </div>
          )}

          {/* Documents Tab */}
          {activeTab === 'docs' && (
            <div className="space-y-3">
              <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><FileText size={16}/> Uploaded Documents ({docs.length})</h3>
              {docs.length === 0 ? (
                <div className="flex items-center justify-center py-12 text-gray-400 gap-2">
                  <FileText size={18} className="opacity-50"/><span className="text-sm">No documents uploaded yet</span>
                </div>
              ) : (
                <div className="grid gap-3">
                  {docs.map(doc => (
                    <div key={doc.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-xl border border-gray-100">
                      <div className="flex items-center gap-3 flex-1 min-w-0">
                        <div className="w-9 h-9 rounded-lg bg-blue-100 flex items-center justify-center flex-shrink-0">
                          <FileText size={16} className="text-blue-600"/>
                        </div>
                        <div className="min-w-0">
                          <p className="font-semibold text-sm text-gray-900 truncate">{doc.originalFileName || doc.fileName}</p>
                          <p className="text-xs text-gray-400">{doc.documentType} · {doc.fileSize ? `${(doc.fileSize / 1024).toFixed(1)} KB` : ''}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3 flex-shrink-0">
                        <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${docStatusStyle[doc.status] || 'bg-gray-100 text-gray-600'}`}>
                          {doc.status || 'Uploaded'}
                        </span>
                        <a href={`/api/documents/${doc.id}`} target="_blank" rel="noreferrer"
                          className="p-2 hover:bg-gray-200 rounded-lg transition-colors" title="Download">
                          <Download size={14} className="text-gray-500"/>
                        </a>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* History Tab */}
          {activeTab === 'history' && (
            <div className="max-w-lg">
              <h3 className="font-bold text-[#0F1B35] mb-5 flex items-center gap-2"><Clock size={16}/> Status Timeline</h3>
              {history.length === 0 ? (
                <div className="flex items-center justify-center py-12 text-gray-400 gap-2">
                  <Clock size={18} className="opacity-50"/><span className="text-sm">No status changes recorded</span>
                </div>
              ) : (
                <div className="relative pl-6">
                  <div className="absolute left-2 top-0 bottom-0 w-0.5 bg-gray-200"/>
                  {history.map((h, i) => (
                    <div key={h.id || i} className="relative mb-6 last:mb-0">
                      <div className={`absolute -left-6 w-4 h-4 rounded-full border-2 border-white ${
                        h.newStatus === 'APPROVED' || h.newStatus === 'COMPLETED' ? 'bg-green-500' :
                        h.newStatus === 'REJECTED' ? 'bg-red-500' : 'bg-[#1E88E5]'
                      }`}/>
                      <div className="bg-gray-50 rounded-xl p-4">
                        <div className="flex items-center justify-between mb-1">
                          <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${statusStyle[h.newStatus] || 'bg-gray-100 text-gray-600'}`}>
                            {h.newStatus?.replace(/_/g,' ')}
                          </span>
                          <span className="text-xs text-gray-400">
                            {h.changedAt ? new Date(h.changedAt).toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'}) : '—'}
                          </span>
                        </div>
                        {h.previousStatus && <p className="text-xs text-gray-400 mt-1">From: {h.previousStatus?.replace(/_/g,' ')}</p>}
                        {h.remarks && <p className="text-sm text-gray-700 mt-2 italic">"{h.remarks}"</p>}
                        {h.changedByName && <p className="text-xs text-gray-400 mt-1">By: {h.changedByName}</p>}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ApplicationDetailPage;
