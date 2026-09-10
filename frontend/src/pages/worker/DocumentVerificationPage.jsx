import React, { useState, useEffect, useCallback } from 'react';
import { FileText, CheckCircle, XCircle, Download, Search, RefreshCw, AlertCircle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getAssignedApplications } from '../../api/applicationApi';
import { getDocuments, updateDocumentStatus } from '../../api/documentApi';

const docStatusStyle = {
  UPLOADED:     'bg-blue-100 text-blue-700',
  UNDER_REVIEW: 'bg-yellow-100 text-yellow-700',
  APPROVED:     'bg-green-100 text-green-700',
  REJECTED:     'bg-red-100 text-red-600',
};

const DocumentVerificationPage = () => {
  const [allDocs, setAllDocs]   = useState([]);
  const [loading, setLoading]   = useState(true);
  const [search, setSearch]     = useState('');
  const [filter, setFilter]     = useState('UPLOADED');
  const [updating, setUpdating] = useState(null);

  const fetchDocs = useCallback(async () => {
    setLoading(true);
    try {
      // Get all assigned applications then collect their documents
      const appsRes = await getAssignedApplications();
      const apps = appsRes.data?.data || [];
      const docResults = await Promise.allSettled(
        apps.map(a => getDocuments(a.id).then(r => ({
          appNum: a.applicationNumber,
          appId: a.id,
          customerName: a.customerName,
          docs: r.data?.data || [],
        })))
      );
      const flat = docResults
        .filter(r => r.status === 'fulfilled')
        .flatMap(r => r.value.docs.map(d => ({
          ...d,
          appNum: r.value.appNum,
          appId: r.value.appId,
          customerName: r.value.customerName,
        })));
      setAllDocs(flat);
    } catch { toast.error('Failed to load documents'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchDocs(); }, [fetchDocs]);

  const handleAction = async (docId, status) => {
    setUpdating(docId);
    try {
      await updateDocumentStatus(docId, status, status === 'REJECTED' ? 'Rejected after review' : '');
      toast.success(`Document ${status.toLowerCase()}`);
      setAllDocs(prev => prev.map(d => d.id === docId ? { ...d, status } : d));
    } catch { toast.error('Failed to update document'); }
    finally { setUpdating(null); }
  };

  const filtered = allDocs.filter(d => {
    const q = search.toLowerCase();
    const matchQ = !q || d.originalFileName?.toLowerCase().includes(q)
      || d.documentType?.toLowerCase().includes(q)
      || d.customerName?.toLowerCase().includes(q)
      || d.appNum?.toLowerCase().includes(q);
    const matchF = !filter || d.status === filter;
    return matchQ && matchF;
  });

  const counts = {
    UPLOADED:     allDocs.filter(d => d.status === 'UPLOADED').length,
    UNDER_REVIEW: allDocs.filter(d => d.status === 'UNDER_REVIEW').length,
    APPROVED:     allDocs.filter(d => d.status === 'APPROVED').length,
    REJECTED:     allDocs.filter(d => d.status === 'REJECTED').length,
  };

  const StatChip = ({ label, count, value, color }) => (
    <button onClick={() => setFilter(filter === value ? '' : value)}
      className={`flex items-center gap-2 px-4 py-2 rounded-xl border text-sm font-semibold transition-all ${
        filter === value ? `${color} border-transparent shadow-sm` : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'
      }`}>
      {label} <span className="text-xs font-bold bg-white/50 px-1.5 py-0.5 rounded-full">{count}</span>
    </button>
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <FileText className="text-[#1E88E5]" size={24}/> Document Verification
          </h1>
          <p className="text-gray-500 text-sm mt-1">{allDocs.length} documents across your assigned applications</p>
        </div>
        <button onClick={fetchDocs} className="p-2 hover:bg-gray-100 rounded-lg self-start" title="Refresh">
          <RefreshCw size={18} className="text-gray-500"/>
        </button>
      </div>

      {/* Status filter chips */}
      <div className="flex flex-wrap gap-2">
        <StatChip label="Pending"     count={counts.UPLOADED}     value="UPLOADED"     color="bg-blue-100 text-blue-700"   />
        <StatChip label="In Review"   count={counts.UNDER_REVIEW} value="UNDER_REVIEW" color="bg-yellow-100 text-yellow-700"/>
        <StatChip label="Approved"    count={counts.APPROVED}     value="APPROVED"     color="bg-green-100 text-green-700"  />
        <StatChip label="Rejected"    count={counts.REJECTED}     value="REJECTED"     color="bg-red-100 text-red-600"      />
        <button onClick={() => setFilter('')}
          className={`px-4 py-2 rounded-xl border text-sm font-semibold transition-all ${
            !filter ? 'bg-gray-800 text-white border-transparent' : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'
          }`}>
          All ({allDocs.length})
        </button>
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
        <input value={search} onChange={e => setSearch(e.target.value)}
          placeholder="Search by name, type, app#..."
          className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
      </div>

      {/* Documents list */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400 gap-3">
            <AlertCircle size={40} className="opacity-25"/>
            <p className="text-sm font-medium">{search || filter ? 'No documents match filters' : 'No documents to verify'}</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-50">
            {filtered.map(doc => (
              <div key={doc.id} className="flex items-center justify-between p-5 hover:bg-gray-50/50 transition-colors gap-4">
                {/* Left: doc info */}
                <div className="flex items-center gap-4 flex-1 min-w-0">
                  <div className="w-10 h-10 rounded-xl bg-blue-50 flex items-center justify-center flex-shrink-0">
                    <FileText size={18} className="text-blue-500"/>
                  </div>
                  <div className="min-w-0">
                    <p className="font-semibold text-sm text-gray-900 truncate">{doc.originalFileName || doc.fileName}</p>
                    <p className="text-xs text-gray-400 mt-0.5">
                      <span className="font-medium text-gray-600">{doc.documentType?.replace(/_/g,' ')}</span>
                      {' · '}{doc.customerName}
                      {' · '}<span className="font-mono">{doc.appNum}</span>
                    </p>
                    {doc.uploadedAt && (
                      <p className="text-xs text-gray-300 mt-0.5">
                        Uploaded {new Date(doc.uploadedAt).toLocaleDateString('en-IN')}
                        {doc.fileSize && ` · ${(doc.fileSize / 1024).toFixed(1)} KB`}
                      </p>
                    )}
                  </div>
                </div>

                {/* Right: status + actions */}
                <div className="flex items-center gap-3 flex-shrink-0">
                  <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${docStatusStyle[doc.status] || 'bg-gray-100 text-gray-600'}`}>
                    {doc.status || 'Uploaded'}
                  </span>
                  <a href={`/api/documents/${doc.id}`} target="_blank" rel="noreferrer"
                    className="p-2 hover:bg-gray-100 rounded-lg transition-colors" title="Download">
                    <Download size={15} className="text-gray-500"/>
                  </a>
                  {doc.status !== 'APPROVED' && (
                    <button
                      disabled={updating === doc.id}
                      onClick={() => handleAction(doc.id, 'APPROVED')}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-green-50 hover:bg-green-100 text-green-700 rounded-lg text-xs font-semibold transition-colors disabled:opacity-50">
                      <CheckCircle size={13}/> Approve
                    </button>
                  )}
                  {doc.status !== 'REJECTED' && (
                    <button
                      disabled={updating === doc.id}
                      onClick={() => handleAction(doc.id, 'REJECTED')}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 rounded-lg text-xs font-semibold transition-colors disabled:opacity-50">
                      <XCircle size={13}/> Reject
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default DocumentVerificationPage;
