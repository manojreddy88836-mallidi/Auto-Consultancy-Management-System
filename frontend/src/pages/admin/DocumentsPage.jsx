import React, { useState, useEffect, useCallback } from 'react';
import { FileText, Search, Filter, Download, CheckCircle, XCircle, RefreshCw, AlertCircle, Eye } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import { getAllApplicationsAdmin } from '../../api/applicationApi';
import { getDocuments, updateDocumentStatus } from '../../api/documentApi';

const docStatusStyle = {
  UPLOADED:     'bg-blue-100 text-blue-700',
  UNDER_REVIEW: 'bg-yellow-100 text-yellow-700',
  APPROVED:     'bg-green-100 text-green-700',
  REJECTED:     'bg-red-100 text-red-600',
};

const AdminDocumentsPage = () => {
  const navigate = useNavigate();
  const [allDocs, setAllDocs]     = useState([]);
  const [loading, setLoading]     = useState(true);
  const [search, setSearch]       = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter]     = useState('');
  const [updating, setUpdating]   = useState(null);

  const fetchDocs = useCallback(async () => {
    setLoading(true);
    try {
      // Fetch recent applications and their documents
      const appsRes = await getAllApplicationsAdmin({ page: 0, size: 50, sort: 'createdAt,desc' });
      const apps = (appsRes.data?.data?.content || appsRes.data?.data || []);
      const results = await Promise.allSettled(
        apps.map(a => getDocuments(a.id).then(r => ({
          appNum: a.applicationNumber, appId: a.id,
          customerName: a.customerName, customerPhone: a.customerPhone,
          docs: r.data?.data || [],
        })))
      );
      const flat = results
        .filter(r => r.status === 'fulfilled')
        .flatMap(r => r.value.docs.map(d => ({
          ...d,
          appNum: r.value.appNum, appId: r.value.appId,
          customerName: r.value.customerName, customerPhone: r.value.customerPhone,
        })));
      setAllDocs(flat);
    } catch { toast.error('Failed to load documents'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchDocs(); }, [fetchDocs]);

  const handleAction = async (docId, status) => {
    setUpdating(docId);
    try {
      await updateDocumentStatus(docId, status, '');
      toast.success(`Document ${status.toLowerCase()}`);
      setAllDocs(prev => prev.map(d => d.id === docId ? { ...d, status } : d));
    } catch { toast.error('Failed to update document'); }
    finally { setUpdating(null); }
  };

  const docTypes = [...new Set(allDocs.map(d => d.documentType).filter(Boolean))].sort();
  const counts = {
    total:    allDocs.length,
    pending:  allDocs.filter(d => d.status === 'UPLOADED' || d.status === 'UNDER_REVIEW').length,
    approved: allDocs.filter(d => d.status === 'APPROVED').length,
    rejected: allDocs.filter(d => d.status === 'REJECTED').length,
  };

  const filtered = allDocs.filter(d => {
    const q = search.toLowerCase();
    const matchQ = !q || d.originalFileName?.toLowerCase().includes(q)
      || d.customerName?.toLowerCase().includes(q)
      || d.appNum?.toLowerCase().includes(q)
      || d.documentType?.toLowerCase().includes(q);
    const matchStatus = !statusFilter || d.status === statusFilter;
    const matchType   = !typeFilter   || d.documentType === typeFilter;
    return matchQ && matchStatus && matchType;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <FileText className="text-[#1E88E5]" size={24}/> All Documents
          </h1>
          <p className="text-gray-500 text-sm mt-1">{counts.total} documents across all applications</p>
        </div>
        <button onClick={fetchDocs} className="p-2 hover:bg-gray-100 rounded-lg self-start" title="Refresh">
          <RefreshCw size={18} className="text-gray-500"/>
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {[
          { label:'Total',    count: counts.total,    bg:'bg-gray-100',    text:'text-gray-700'   },
          { label:'Pending',  count: counts.pending,  bg:'bg-yellow-50',   text:'text-yellow-700' },
          { label:'Approved', count: counts.approved, bg:'bg-green-50',    text:'text-green-700'  },
          { label:'Rejected', count: counts.rejected, bg:'bg-red-50',      text:'text-red-600'    },
        ].map(s => (
          <div key={s.label} className={`${s.bg} rounded-2xl p-4 text-center border border-current/5`}>
            <p className={`text-2xl font-bold ${s.text}`}>{loading ? '—' : s.count}</p>
            <p className={`text-xs font-semibold mt-0.5 ${s.text}`}>{s.label}</p>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 flex-wrap">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search docs, customer, app#..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
        </div>
        <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
          <option value="">All Statuses</option>
          <option value="UPLOADED">Pending Review</option>
          <option value="UNDER_REVIEW">Under Review</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
        <select value={typeFilter} onChange={e => setTypeFilter(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
          <option value="">All Types</option>
          {docTypes.map(t => <option key={t} value={t}>{t.replace(/_/g,' ')}</option>)}
        </select>
      </div>

      {/* Document list */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400 gap-3">
            <AlertCircle size={40} className="opacity-25"/>
            <p className="text-sm font-medium">No documents match your filters</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['Document','Type','Customer','App #','Uploaded','Status','Actions'].map(h => (
                    <th key={h} className="text-left px-5 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map(doc => (
                  <tr key={doc.id} className="hover:bg-gray-50/70 transition-colors">
                    <td className="px-5 py-4">
                      <p className="font-semibold text-gray-900 text-xs truncate max-w-[160px]">{doc.originalFileName || doc.fileName}</p>
                      {doc.fileSize && <p className="text-xs text-gray-300">{(doc.fileSize/1024).toFixed(1)} KB</p>}
                    </td>
                    <td className="px-5 py-4">
                      <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700">
                        {doc.documentType?.replace(/_/g,' ')}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <p className="font-semibold text-gray-900 text-xs whitespace-nowrap">{doc.customerName}</p>
                      <p className="text-xs text-gray-400">{doc.customerPhone}</p>
                    </td>
                    <td className="px-5 py-4 font-mono text-xs text-gray-400 whitespace-nowrap">{doc.appNum}</td>
                    <td className="px-5 py-4 text-xs text-gray-400 whitespace-nowrap">
                      {doc.uploadedAt ? new Date(doc.uploadedAt).toLocaleDateString('en-IN') : '—'}
                    </td>
                    <td className="px-5 py-4">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${docStatusStyle[doc.status] || 'bg-gray-100 text-gray-600'}`}>
                        {doc.status === 'UPLOADED' ? 'Pending' : doc.status?.replace(/_/g,' ') || '—'}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-1.5">
                        <a href={`/api/documents/${doc.id}`} target="_blank" rel="noreferrer"
                          className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors" title="Download">
                          <Download size={14} className="text-gray-500"/>
                        </a>
                        <button onClick={() => navigate(`/admin/applications/${doc.appId}?tab=docs`)}
                          className="p-1.5 hover:bg-blue-50 rounded-lg transition-colors" title="View Application">
                          <Eye size={14} className="text-blue-500"/>
                        </button>
                        {doc.status !== 'APPROVED' && (
                          <button disabled={updating === doc.id} onClick={() => handleAction(doc.id, 'APPROVED')}
                            className="p-1.5 hover:bg-green-100 rounded-lg transition-colors disabled:opacity-50" title="Approve">
                            <CheckCircle size={14} className="text-green-600"/>
                          </button>
                        )}
                        {doc.status !== 'REJECTED' && (
                          <button disabled={updating === doc.id} onClick={() => handleAction(doc.id, 'REJECTED')}
                            className="p-1.5 hover:bg-red-100 rounded-lg transition-colors disabled:opacity-50" title="Reject">
                            <XCircle size={14} className="text-red-500"/>
                          </button>
                        )}
                      </div>
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

export default AdminDocumentsPage;
