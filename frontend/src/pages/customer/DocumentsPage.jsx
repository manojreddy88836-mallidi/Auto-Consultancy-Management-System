import React, { useState, useEffect, useCallback } from 'react';
import { FileText, Upload, Download, CheckCircle, XCircle, Clock, AlertCircle, ChevronDown, ChevronUp } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getMyApplications } from '../../api/applicationApi';
import { getDocuments, uploadDocument } from '../../api/documentApi';

const DOC_TYPES = [
  'AADHAAR','PAN','DL','RC','INSURANCE','FINANCE',
  'LOAN_STATEMENT','NOC','ADDRESS_PROOF','OTHER'
];
const docStatusStyle = {
  UPLOADED:     'bg-blue-100 text-blue-700',
  UNDER_REVIEW: 'bg-yellow-100 text-yellow-700',
  APPROVED:     'bg-green-100 text-green-700',
  REJECTED:     'bg-red-100 text-red-600',
};
const docStatusIcon = {
  APPROVED: CheckCircle,
  REJECTED: XCircle,
  UPLOADED: Clock,
  UNDER_REVIEW: Clock,
};

const CustomerDocumentsPage = () => {
  const [apps, setApps]           = useState([]);
  const [docsMap, setDocsMap]     = useState({});
  const [loading, setLoading]     = useState(true);
  const [expanded, setExpanded]   = useState({});
  const [uploading, setUploading] = useState(null);
  const [uploadForm, setUploadForm] = useState({ appId: '', docType: '', file: null });
  const [showUploadFor, setShowUploadFor] = useState(null);

  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const appsRes = await getMyApplications();
      const myApps = (appsRes.data?.data || []).filter(a => a.status !== 'DRAFT');
      setApps(myApps);

      const docResults = await Promise.allSettled(
        myApps.map(a => getDocuments(a.id).then(r => ({ appId: a.id, docs: r.data?.data || [] })))
      );
      const map = {};
      docResults.forEach(r => { if (r.status === 'fulfilled') map[r.value.appId] = r.value.docs; });
      setDocsMap(map);
      // Auto-expand first app
      if (myApps.length > 0) setExpanded({ [myApps[0].id]: true });
    } catch { toast.error('Failed to load documents'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const toggleExpand = (id) => setExpanded(prev => ({ ...prev, [id]: !prev[id] }));

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!uploadForm.appId || !uploadForm.docType || !uploadForm.file) {
      toast.error('Please fill all fields and select a file');
      return;
    }
    setUploading(uploadForm.appId);
    try {
      const fd = new FormData();
      fd.append('file', uploadForm.file);
      fd.append('documentType', uploadForm.docType);
      await uploadDocument(uploadForm.appId, fd);
      toast.success('Document uploaded successfully');
      setShowUploadFor(null);
      setUploadForm({ appId: '', docType: '', file: null });
      // Refresh docs for this app
      const res = await getDocuments(uploadForm.appId);
      setDocsMap(prev => ({ ...prev, [uploadForm.appId]: res.data?.data || [] }));
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Upload failed');
    } finally { setUploading(null); }
  };

  const totalDocs = Object.values(docsMap).flat().length;
  const approvedDocs = Object.values(docsMap).flat().filter(d => d.status === 'APPROVED').length;

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
    </div>
  );

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">My Documents</h1>
        <p className="text-gray-500 text-sm mt-1">{totalDocs} documents · {approvedDocs} approved</p>
      </div>

      {/* Summary cards */}
      {totalDocs > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {[
            { label: 'Total',       count: totalDocs,                                                               color: 'bg-blue-50 text-blue-700'    },
            { label: 'Approved',    count: Object.values(docsMap).flat().filter(d=>d.status==='APPROVED').length,   color: 'bg-green-50 text-green-700'  },
            { label: 'Pending',     count: Object.values(docsMap).flat().filter(d=>d.status==='UPLOADED'||d.status==='UNDER_REVIEW').length, color: 'bg-yellow-50 text-yellow-700'},
            { label: 'Rejected',    count: Object.values(docsMap).flat().filter(d=>d.status==='REJECTED').length,   color: 'bg-red-50 text-red-600'      },
          ].map(s => (
            <div key={s.label} className={`rounded-2xl p-4 text-center ${s.color} border border-current/10`}>
              <p className="text-2xl font-bold">{s.count}</p>
              <p className="text-xs font-semibold mt-0.5">{s.label}</p>
            </div>
          ))}
        </div>
      )}

      {/* No submitted apps */}
      {apps.length === 0 ? (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-12 flex flex-col items-center text-center">
          <div className="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mb-4">
            <FileText size={28} className="text-[#1E88E5] opacity-60"/>
          </div>
          <h3 className="text-base font-bold text-gray-900 mb-1">No submitted applications</h3>
          <p className="text-gray-500 text-sm">Submit a bike application first to manage documents.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {apps.map(app => {
            const docs = docsMap[app.id] || [];
            const isOpen = expanded[app.id];
            const isUploading = uploading === app.id;
            const showForm = showUploadFor === app.id;
            return (
              <div key={app.id} className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
                {/* App header */}
                <div
                  className="flex items-center justify-between p-5 cursor-pointer hover:bg-gray-50/50 transition-colors"
                  onClick={() => toggleExpand(app.id)}>
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-blue-50 flex items-center justify-center flex-shrink-0">
                      <FileText size={16} className="text-blue-500"/>
                    </div>
                    <div>
                      <p className="font-bold text-gray-900 text-sm">
                        {app.manufacturerName && app.modelName
                          ? `${app.manufacturerName} ${app.modelName}`
                          : app.applicationNumber}
                      </p>
                      <p className="text-xs text-gray-400 font-mono">{app.applicationNumber}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-gray-500">{docs.length} doc{docs.length !== 1 ? 's' : ''}</span>
                    {isOpen ? <ChevronUp size={16} className="text-gray-400"/> : <ChevronDown size={16} className="text-gray-400"/>}
                  </div>
                </div>

                {/* Documents list */}
                {isOpen && (
                  <div className="border-t border-gray-100">
                    {docs.length === 0 ? (
                      <div className="flex flex-col items-center justify-center py-8 text-gray-400 gap-2">
                        <AlertCircle size={24} className="opacity-30"/>
                        <p className="text-xs">No documents uploaded for this application</p>
                      </div>
                    ) : (
                      <div className="divide-y divide-gray-50">
                        {docs.map(doc => {
                          const StatusIcon = docStatusIcon[doc.status] || Clock;
                          return (
                            <div key={doc.id} className="flex items-center justify-between px-5 py-3.5 gap-3">
                              <div className="flex items-center gap-3 flex-1 min-w-0">
                                <StatusIcon size={16} className={
                                  doc.status === 'APPROVED' ? 'text-green-500' :
                                  doc.status === 'REJECTED' ? 'text-red-500' : 'text-gray-400'
                                }/>
                                <div className="min-w-0">
                                  <p className="font-medium text-sm text-gray-900 truncate">{doc.originalFileName || doc.fileName}</p>
                                  <p className="text-xs text-gray-400">{doc.documentType?.replace(/_/g,' ')}</p>
                                </div>
                              </div>
                              <div className="flex items-center gap-2 flex-shrink-0">
                                <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${docStatusStyle[doc.status] || 'bg-gray-100 text-gray-500'}`}>
                                  {doc.status === 'UPLOADED' ? 'Pending' : doc.status?.replace(/_/g,' ')}
                                </span>
                                <a href={`/api/documents/${doc.id}`} target="_blank" rel="noreferrer"
                                  className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors" title="Download">
                                  <Download size={14} className="text-gray-400"/>
                                </a>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}

                    {/* Upload section */}
                    <div className="px-5 pb-4 pt-2 border-t border-gray-50">
                      {!showForm ? (
                        <button
                          onClick={() => { setShowUploadFor(app.id); setUploadForm({ appId: app.id, docType: '', file: null }); }}
                          className="flex items-center gap-2 px-4 py-2 text-sm font-semibold text-[#1E88E5] hover:bg-blue-50 rounded-xl transition-colors">
                          <Upload size={15}/> Upload Document
                        </button>
                      ) : (
                        <form onSubmit={handleUpload} className="flex flex-col sm:flex-row gap-3 items-start sm:items-end mt-2">
                          <div className="flex-1">
                            <label className="block text-xs font-semibold text-gray-600 mb-1">Document Type</label>
                            <select value={uploadForm.docType}
                              onChange={e => setUploadForm(f => ({ ...f, docType: e.target.value }))}
                              className="w-full px-3 py-2 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
                              <option value="">Select type</option>
                              {DOC_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g,' ')}</option>)}
                            </select>
                          </div>
                          <div className="flex-1">
                            <label className="block text-xs font-semibold text-gray-600 mb-1">File</label>
                            <input type="file" accept=".pdf,.jpg,.jpeg,.png"
                              onChange={e => setUploadForm(f => ({ ...f, file: e.target.files[0] }))}
                              className="w-full text-xs text-gray-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:bg-blue-50 file:text-blue-700 file:font-semibold hover:file:bg-blue-100"/>
                          </div>
                          <div className="flex gap-2">
                            <button type="submit" disabled={isUploading}
                              className="px-4 py-2 bg-[#1E88E5] hover:bg-[#1976D2] text-white text-sm font-semibold rounded-xl transition-all disabled:opacity-60">
                              {isUploading ? 'Uploading…' : 'Upload'}
                            </button>
                            <button type="button" onClick={() => setShowUploadFor(null)}
                              className="px-4 py-2 border border-gray-200 text-sm text-gray-600 hover:bg-gray-50 rounded-xl transition-colors">
                              Cancel
                            </button>
                          </div>
                        </form>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default CustomerDocumentsPage;
