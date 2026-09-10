import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import {
  ArrowLeft, Bike, CreditCard, FileText, Clock, User,
  AlertCircle, Download, CheckCircle, XCircle
} from 'lucide-react';
import { getApplicationById, updateApplicationStatus } from '../../api/applicationApi';
import { getDocuments, updateDocumentStatus } from '../../api/documentApi';

const BACKEND_BASE = 'http://localhost:8080';

const ALL_STATUSES = [
  'UNDER_REVIEW','DOCUMENT_VERIFICATION','FINANCE_VERIFICATION','APPROVED','REJECTED','COMPLETED'
];
const statusStyle = {
  SUBMITTED:'bg-blue-100 text-blue-700', UNDER_REVIEW:'bg-yellow-100 text-yellow-700',
  DOCUMENT_VERIFICATION:'bg-purple-100 text-purple-700', FINANCE_VERIFICATION:'bg-orange-100 text-orange-700',
  WORKER_ASSIGNED:'bg-indigo-100 text-indigo-700', APPROVED:'bg-green-100 text-green-700',
  REJECTED:'bg-red-100 text-red-600', COMPLETED:'bg-emerald-100 text-emerald-700',
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
  { id:'overview', label:'Overview',       icon: User     },
  { id:'bike',     label:'Bike Details',   icon: Bike     },
  { id:'finance',  label:'Finance',        icon: CreditCard},
  { id:'docs',     label:'Documents',      icon: FileText },
  { id:'history',  label:'Status History', icon: Clock    },
];

const WorkerApplicationDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [app, setApp]             = useState(null);
  const [docs, setDocs]           = useState([]);
  const [loading, setLoading]     = useState(true);
  const [activeTab, setActiveTab] = useState('overview');
  const [newStatus, setNewStatus] = useState('');
  const [remarks, setRemarks]     = useState('');
  const [saving, setSaving]       = useState(false);

  const refresh = async () => {
    try {
      const [appRes, docsRes] = await Promise.all([
        getApplicationById(id),
        getDocuments(id),
      ]);
      const d = appRes.data?.data || appRes.data;
      setApp(d);
      setNewStatus(d?.status || '');
      setDocs(docsRes.data?.data || []);
    } catch { toast.error('Failed to load application'); }
    finally { setLoading(false); }
  };

  useEffect(() => { refresh(); }, [id]);

  const handleUpdateStatus = async () => {
    if (!newStatus) { toast.error('Select a status'); return; }
    setSaving(true);
    try {
      await updateApplicationStatus(id, { status: newStatus, remarks });
      toast.success('Status updated successfully');
      setRemarks('');
      await refresh();
    } catch (e) { toast.error(e?.response?.data?.message || 'Update failed'); }
    finally { setSaving(false); }
  };

  const handleDocAction = async (docId, action) => {
    try {
      await updateDocumentStatus(docId, action, action === 'REJECTED' ? 'Rejected by worker' : '');
      toast.success(`Document ${action.toLowerCase()}`);
      const docsRes = await getDocuments(id);
      setDocs(docsRes.data?.data || []);
    } catch { toast.error('Failed to update document'); }
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
      <button onClick={() => navigate('/worker/applications')} className="text-[#1E88E5] text-sm hover:underline">← Back</button>
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
          <button onClick={() => navigate('/worker/applications')}
            className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 mb-3 transition-colors">
            <ArrowLeft size={15}/> Back to Applications
          </button>
          <h1 className="text-2xl font-bold text-gray-900">{app.applicationNumber}</h1>
          <div className="flex items-center gap-3 mt-2">
            <span className={`px-3 py-1 rounded-full text-xs font-bold ${statusStyle[app.status] || 'bg-gray-100 text-gray-600'}`}>
              {app.status?.replace(/_/g, ' ')}
            </span>
            <span className="text-xs text-gray-400">Customer: <strong className="text-gray-700">{app.customer?.fullName}</strong></span>
          </div>
        </div>

        {/* Status update panel */}
        <div className="bg-white rounded-2xl border border-gray-200 p-4 shadow-sm space-y-3 min-w-[220px]">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Update Status</p>
          <select value={newStatus} onChange={e => setNewStatus(e.target.value)}
            className="w-full px-3 py-2 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
            {ALL_STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g,' ')}</option>)}
          </select>
          <textarea rows={2} value={remarks} onChange={e => setRemarks(e.target.value)}
            placeholder="Remarks (optional)"
            className="w-full px-3 py-2 rounded-xl border border-gray-200 text-xs resize-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
          <button onClick={handleUpdateStatus} disabled={saving}
            className="w-full py-2 bg-[#0F1B35] hover:bg-[#1E3A5F] text-white rounded-xl text-sm font-semibold transition-all disabled:opacity-60">
            {saving ? 'Updating…' : 'Update Status'}
          </button>
        </div>
      </div>

      {/* Tabbed content */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="flex border-b border-gray-100 overflow-x-auto">
          {TABS.map(tab => {
            const Icon = tab.icon;
            return (
              <button key={tab.id} onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-5 py-4 text-sm font-medium whitespace-nowrap border-b-2 transition-colors ${
                  activeTab === tab.id ? 'border-[#1E88E5] text-[#1E88E5]' : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}>
                <Icon size={14}/> {tab.label}
              </button>
            );
          })}
        </div>

        <div className="p-6">
          {/* Overview */}
          {activeTab === 'overview' && (
            <div className="grid md:grid-cols-2 gap-5">
              <div className="bg-gray-50 rounded-xl p-5">
                <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><User size={15}/> Customer Info</h3>
                <InfoRow label="Name"    value={app.customer?.fullName}/>
                <InfoRow label="Email"   value={app.customer?.email}/>
                <InfoRow label="Phone"   value={app.customer?.phone}/>
                <InfoRow label="City"    value={app.customer?.city}/>
              </div>
              <div className="bg-gray-50 rounded-xl p-5">
                <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><Bike size={15}/> Bike Summary</h3>
                {app.bikeImageUrl && (
                  <div className="mb-3 rounded-xl overflow-hidden aspect-[16/9] max-h-36 bg-gray-200">
                    <img src={`${BACKEND_BASE}${app.bikeImageUrl}`} alt="Bike" className="w-full h-full object-cover" />
                  </div>
                )}
                {app.bikeSaleStatus && app.bikeSaleStatus !== 'NOT_FOR_SALE' && (
                  <div className="mb-2">
                    <span className={`px-2 py-0.5 rounded text-xs font-semibold ${
                      app.bikeSaleStatus === 'AVAILABLE' ? 'bg-green-100 text-green-700' :
                      app.bikeSaleStatus === 'RESERVED'  ? 'bg-amber-100 text-amber-700' :
                      app.bikeSaleStatus === 'SOLD'      ? 'bg-red-100 text-red-600' : 'bg-gray-100 text-gray-500'
                    }`}>{app.bikeSaleStatus}</span>
                  </div>
                )}
                <InfoRow label="Brand" value={app.bikeDetail?.manufacturerName}/>
                <InfoRow label="Model"        value={app.bikeDetail?.modelName}/>
                <InfoRow label="Year"         value={app.bikeDetail?.manufacturingYear}/>
                <InfoRow label="Reg No"       value={app.bikeDetail?.registrationNumber}/>
              </div>
              <div className="bg-gray-50 rounded-xl p-5">
                <h3 className="font-bold text-[#0F1B35] mb-4">Application Info</h3>
                <InfoRow label="App Number"  value={app.applicationNumber}/>
                <InfoRow label="Submitted"   value={app.submittedAt ? new Date(app.submittedAt).toLocaleDateString('en-IN') : '—'}/>
                <InfoRow label="Last Update" value={app.updatedAt ? new Date(app.updatedAt).toLocaleDateString('en-IN') : '—'}/>
                <InfoRow label="Finance"     value={app.financeDetail?.underFinance === true ? 'Yes (Under Finance)' : app.financeDetail?.underFinance === false ? 'No (Paid)' : '—'}/>
              </div>
            </div>
          )}

          {/* Bike */}
          {activeTab === 'bike' && (bd ? (
            <div className="max-w-2xl">
              {app.bikeImageUrl && (
                <div className="mb-4 rounded-2xl overflow-hidden aspect-[16/9] max-h-48 bg-gray-100 border border-gray-200">
                  <img src={`${BACKEND_BASE}${app.bikeImageUrl}`} alt="Bike" className="w-full h-full object-cover" />
                </div>
              )}
              {app.bikeSaleStatus && app.bikeSaleStatus !== 'NOT_FOR_SALE' && (
                <div className="mb-3">
                  <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                    app.bikeSaleStatus === 'AVAILABLE' ? 'bg-green-100 text-green-700' :
                    app.bikeSaleStatus === 'RESERVED'  ? 'bg-amber-100 text-amber-700' :
                    app.bikeSaleStatus === 'SOLD'      ? 'bg-red-100 text-red-600' : 'bg-gray-100 text-gray-500'
                  }`}>Sale Status: {app.bikeSaleStatus}</span>
                </div>
              )}
              <div className="bg-gray-50 rounded-xl p-5">
                <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><Bike size={15}/> Bike Details</h3>
                <InfoRow label="Brand"        value={bd.manufacturerName}/>
                <InfoRow label="Model"         value={bd.modelName}/>
                <InfoRow label="Variant"       value={bd.variantName}/>
                <InfoRow label="Year"          value={bd.manufacturingYear}/>
                <InfoRow label="Reg Number"    value={bd.registrationNumber}/>
                <InfoRow label="Colour"        value={bd.colour}/>
                <InfoRow label="Purchase Date" value={bd.purchaseDate}/>
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-center py-12 text-gray-400 gap-2">
              <AlertCircle size={18} className="opacity-50"/><span className="text-sm">No bike details submitted yet</span>
            </div>
          ))}

          {/* Finance */}
          {activeTab === 'finance' && (fd ? (
            <div className="max-w-2xl bg-gray-50 rounded-xl p-5">
              <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2"><CreditCard size={15}/> Finance Details</h3>
              <InfoRow label="Under Finance"    value={fd.underFinance ? 'Yes' : 'No'}/>
              {fd.underFinance && <>
                <InfoRow label="Finance Company" value={fd.financeCompany}/>
                <InfoRow label="Loan Account"    value={fd.loanAccountNumber}/>
                <InfoRow label="Loan Amount"     value={fd.loanAmount ? `₹${Number(fd.loanAmount).toLocaleString('en-IN')}` : '—'}/>
                <InfoRow label="EMI Amount"      value={fd.emiAmount ? `₹${Number(fd.emiAmount).toLocaleString('en-IN')}` : '—'}/>
                <InfoRow label="EMIs Paid"       value={`${fd.paidEmis || 0} / ${fd.numberOfEmis || 0}`}/>
                <InfoRow label="Outstanding"     value={fd.outstandingLoanAmount ? `₹${Number(fd.outstandingLoanAmount).toLocaleString('en-IN')}` : '—'}/>
                <InfoRow label="NOC Available"   value={fd.nocAvailable ? 'Yes' : 'No'}/>
                <InfoRow label="Loan Status"     value={fd.loanClosureStatus}/>
              </>}
              {!fd.underFinance && <>
                <InfoRow label="Payment Method" value={fd.purchasePaymentMethod}/>
                <InfoRow label="Bike Paid"      value={fd.bikePaid ? 'Yes' : 'No'}/>
              </>}
            </div>
          ) : (
            <div className="flex items-center justify-center py-12 text-gray-400 gap-2">
              <AlertCircle size={18} className="opacity-50"/><span className="text-sm">No finance details submitted yet</span>
            </div>
          ))}

          {/* Documents */}
          {activeTab === 'docs' && (
            <div>
              <h3 className="font-bold text-[#0F1B35] mb-4 flex items-center gap-2">
                <FileText size={15}/> Documents ({docs.length})
              </h3>
              {docs.length === 0 ? (
                <div className="flex items-center justify-center py-12 text-gray-400 gap-2">
                  <FileText size={18} className="opacity-50"/><span className="text-sm">No documents uploaded</span>
                </div>
              ) : (
                <div className="space-y-3">
                  {docs.map(doc => (
                    <div key={doc.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-xl border border-gray-100 gap-3">
                      <div className="flex items-center gap-3 flex-1 min-w-0">
                        <div className="w-9 h-9 rounded-lg bg-blue-100 flex items-center justify-center flex-shrink-0">
                          <FileText size={16} className="text-blue-600"/>
                        </div>
                        <div className="min-w-0">
                          <p className="font-semibold text-sm text-gray-900 truncate">{doc.originalFileName || doc.fileName}</p>
                          <p className="text-xs text-gray-400">{doc.documentType?.replace(/_/g,' ')} · {doc.fileSize ? `${(doc.fileSize/1024).toFixed(1)} KB` : ''}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2 flex-shrink-0">
                        <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${docStatusStyle[doc.status] || 'bg-gray-100 text-gray-600'}`}>
                          {doc.status || 'Uploaded'}
                        </span>
                        {doc.status !== 'APPROVED' && (
                          <button onClick={() => handleDocAction(doc.id, 'APPROVED')}
                            className="p-1.5 hover:bg-green-100 rounded-lg transition-colors" title="Approve">
                            <CheckCircle size={16} className="text-green-600"/>
                          </button>
                        )}
                        {doc.status !== 'REJECTED' && (
                          <button onClick={() => handleDocAction(doc.id, 'REJECTED')}
                            className="p-1.5 hover:bg-red-100 rounded-lg transition-colors" title="Reject">
                            <XCircle size={16} className="text-red-500"/>
                          </button>
                        )}
                        <a href={`/api/documents/${doc.id}`} target="_blank" rel="noreferrer"
                          className="p-1.5 hover:bg-gray-200 rounded-lg transition-colors" title="Download">
                          <Download size={15} className="text-gray-500"/>
                        </a>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* History */}
          {activeTab === 'history' && (
            <div className="max-w-lg">
              <h3 className="font-bold text-[#0F1B35] mb-5 flex items-center gap-2"><Clock size={15}/> Status Timeline</h3>
              {history.length === 0 ? (
                <div className="flex items-center justify-center py-12 text-gray-400 gap-2">
                  <Clock size={18} className="opacity-50"/><span className="text-sm">No history yet</span>
                </div>
              ) : (
                <div className="relative pl-6">
                  <div className="absolute left-2 top-0 bottom-0 w-0.5 bg-gray-200"/>
                  {history.map((h, i) => (
                    <div key={h.id || i} className="relative mb-5 last:mb-0">
                      <div className={`absolute -left-6 w-4 h-4 rounded-full border-2 border-white ${
                        h.newStatus === 'APPROVED' || h.newStatus === 'COMPLETED' ? 'bg-green-500' :
                        h.newStatus === 'REJECTED' ? 'bg-red-500' : 'bg-[#1E88E5]'
                      }`}/>
                      <div className="bg-gray-50 rounded-xl p-3.5">
                        <div className="flex items-center justify-between mb-1">
                          <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${statusStyle[h.newStatus] || 'bg-gray-100 text-gray-600'}`}>
                            {h.newStatus?.replace(/_/g,' ')}
                          </span>
                          <span className="text-xs text-gray-400">
                            {h.changedAt ? new Date(h.changedAt).toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'}) : '—'}
                          </span>
                        </div>
                        {h.remarks && <p className="text-sm text-gray-700 mt-1.5 italic">"{h.remarks}"</p>}
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

export default WorkerApplicationDetailPage;
