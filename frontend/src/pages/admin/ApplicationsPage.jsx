import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Filter, RefreshCw, Eye, UserCheck, FileText, Bike, Trash2, AlertTriangle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import {
  getAllApplicationsAdmin, updateApplicationStatus, assignWorker, deleteApplication
} from '../../api/applicationApi';
import { getWorkers } from '../../api/adminApi';

const BACKEND_BASE = 'http://localhost:8080';

const ALL_STATUSES = [
  'DRAFT','SUBMITTED','UNDER_REVIEW','DOCUMENT_VERIFICATION',
  'FINANCE_VERIFICATION','WORKER_ASSIGNED','APPROVED','REJECTED','COMPLETED'
];

const statusStyle = {
  DRAFT:                 'bg-gray-100 text-gray-600',
  SUBMITTED:             'bg-blue-100 text-blue-700',
  UNDER_REVIEW:          'bg-yellow-100 text-yellow-700',
  DOCUMENT_VERIFICATION: 'bg-purple-100 text-purple-700',
  FINANCE_VERIFICATION:  'bg-orange-100 text-orange-700',
  WORKER_ASSIGNED:       'bg-indigo-100 text-indigo-700',
  APPROVED:              'bg-green-100 text-green-700',
  REJECTED:              'bg-red-100 text-red-600',
  COMPLETED:             'bg-emerald-100 text-emerald-700',
};

const StatusBadge = ({ status }) => (
  <span className={`px-2.5 py-1 rounded-full text-xs font-semibold whitespace-nowrap ${statusStyle[status] || 'bg-gray-100 text-gray-600'}`}>
    {status?.replace(/_/g, ' ')}
  </span>
);

const ApplicationsPage = () => {
  const navigate = useNavigate();
  const [applications, setApplications]   = useState([]);
  const [loading, setLoading]             = useState(true);
  const [search, setSearch]               = useState('');
  const [statusFilter, setStatusFilter]   = useState('');
  const [page, setPage]                   = useState(0);
  const [totalPages, setTotalPages]       = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [workers, setWorkers]             = useState([]);
  const [assignModalApp, setAssignModalApp]   = useState(null);
  const [selectedWorker, setSelectedWorker]   = useState('');
  const [statusModalApp, setStatusModalApp]   = useState(null);
  const [newStatus, setNewStatus]             = useState('');
  const [remarks, setRemarks]                 = useState('');
  const [deleteModalApp, setDeleteModalApp]   = useState(null);
  const [actionLoading, setActionLoading]     = useState(false);
  const PAGE_SIZE = 10;

  const fetchApplications = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getAllApplicationsAdmin({
        page, size: PAGE_SIZE,
        sort: 'createdAt,desc',
        ...(statusFilter && { status: statusFilter }),
        ...(search.trim() && { search: search.trim() }),
      });
      const d = res.data?.data;
      if (d?.content !== undefined) {
        setApplications(d.content);
        setTotalPages(d.totalPages || 1);
        setTotalElements(d.totalElements || 0);
      } else {
        const arr = Array.isArray(d) ? d : [];
        setApplications(arr);
        setTotalPages(1);
        setTotalElements(arr.length);
      }
    } catch { toast.error('Failed to load applications'); }
    finally { setLoading(false); }
  }, [page, statusFilter, search]);

  useEffect(() => { fetchApplications(); }, [fetchApplications]);

  useEffect(() => {
    getWorkers({ page: 0, size: 100 })
      .then(r => {
        const d = r.data?.data;
        setWorkers(Array.isArray(d) ? d : d?.content || []);
      })
      .catch(() => {});
  }, []);

  const handleAssign = async () => {
    if (!selectedWorker) { toast.error('Please select a worker'); return; }
    setActionLoading(true);
    try {
      await assignWorker(assignModalApp.id, selectedWorker);
      toast.success('Worker assigned successfully');
      setAssignModalApp(null);
      fetchApplications();
    } catch (e) { toast.error(e?.response?.data?.message || 'Assignment failed'); }
    finally { setActionLoading(false); }
  };

  const handleUpdateStatus = async () => {
    if (!newStatus) { toast.error('Select a status'); return; }
    setActionLoading(true);
    try {
      await updateApplicationStatus(statusModalApp.id, { status: newStatus, remarks });
      toast.success('Status updated');
      setStatusModalApp(null); setRemarks('');
      fetchApplications();
    } catch (e) { toast.error(e?.response?.data?.message || 'Update failed'); }
    finally { setActionLoading(false); }
  };

  const handleDelete = async () => {
    if (!deleteModalApp) return;
    setActionLoading(true);
    try {
      await deleteApplication(deleteModalApp.id);
      toast.success(`Application ${deleteModalApp.applicationNumber} deleted`);
      setDeleteModalApp(null);
      // If we deleted the last item on a page > 0, go back one page
      if ((applications || []).length === 1 && page > 0) setPage(p => p - 1);
      else fetchApplications();
    } catch (e) { toast.error(e?.response?.data?.message || 'Delete failed'); }
    finally { setActionLoading(false); }
  };

  const Modal = ({ isOpen, onClose, title, children }) => !isOpen ? null : (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 p-6 z-10">
        <h3 className="text-lg font-bold text-gray-900 mb-5">{title}</h3>
        {children}
      </div>
    </div>
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate(-1)}
            className="p-2 hover:bg-gray-100 rounded-xl transition-colors" title="Go back">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
              className="text-gray-500"><path d="m15 18-6-6 6-6"/></svg>
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
              <FileText className="text-[#1E88E5]" size={24}/> Applications
            </h1>
            <p className="text-gray-500 text-sm mt-1">{totalElements} total applications</p>
          </div>
        </div>
        <button onClick={fetchApplications} className="p-2 hover:bg-gray-100 rounded-lg self-start" title="Refresh">
          <RefreshCw size={18} className="text-gray-500"/>
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(0); }}
            placeholder="Search by customer, app#..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
        </div>
        <div className="flex items-center gap-2">
          <Filter size={15} className="text-gray-400"/>
          <select value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(0); }}
            className="px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
            <option value="">All Statuses</option>
            {ALL_STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g,' ')}</option>)}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
          </div>
        ) : (applications || []).length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400">
            <FileText size={40} className="mb-3 opacity-25"/>
            <p className="text-sm font-medium">No applications found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['App #','Customer','Bike','Finance','Status','Worker','Submitted','Actions'].map(h => (
                    <th key={h} className="text-left px-5 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {(applications || []).map(app => (
                  <tr key={app.id} className="hover:bg-gray-50/70 transition-colors">
                    <td className="px-5 py-4 font-mono text-xs text-gray-500 whitespace-nowrap">{app.applicationNumber}</td>
                    <td className="px-5 py-4">
                      <p className="font-semibold text-gray-900 whitespace-nowrap">{app.customerName}</p>
                      <p className="text-xs text-gray-400">{app.customerPhone}</p>
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        <div className="shrink-0 w-10 h-10 rounded-lg overflow-hidden bg-gray-100 border border-gray-200 flex items-center justify-center">
                          {app.bikeImageUrl ? (
                            <img src={`${BACKEND_BASE}${app.bikeImageUrl}`} alt="" className="w-full h-full object-cover" loading="lazy" />
                          ) : (
                            <Bike size={16} className="text-gray-300" />
                          )}
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-800 whitespace-nowrap">
                            {app.manufacturerName} {app.modelName}
                          </p>
                          {app.manufacturingYear && <p className="text-xs text-gray-400">{app.manufacturingYear}</p>}
                          {app.bikeSaleStatus && app.bikeSaleStatus !== 'NOT_FOR_SALE' && (
                            <span className={`text-xs font-medium ${
                              app.bikeSaleStatus === 'AVAILABLE' ? 'text-green-600' :
                              app.bikeSaleStatus === 'RESERVED' ? 'text-amber-600' :
                              app.bikeSaleStatus === 'SOLD' ? 'text-red-500' : 'text-gray-400'
                            }`}>{app.bikeSaleStatus}</span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                        app.underFinance === true ? 'bg-orange-100 text-orange-700' :
                        app.underFinance === false ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                      }`}>
                        {app.underFinance === true ? 'Financed' : app.underFinance === false ? 'Paid' : 'N/A'}
                      </span>
                    </td>
                    <td className="px-5 py-4"><StatusBadge status={app.status}/></td>
                    <td className="px-5 py-4 text-xs text-gray-500 whitespace-nowrap">
                      {app.assignedWorkerName || <span className="text-gray-300 italic">Unassigned</span>}
                    </td>
                    <td className="px-5 py-4 text-xs text-gray-400 whitespace-nowrap">
                      {app.submittedAt ? new Date(app.submittedAt).toLocaleDateString('en-IN') : 'â€”'}
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-1">
                        <button onClick={() => navigate(`/admin/applications/${app.id}`)}
                          className="p-1.5 hover:bg-blue-50 rounded-lg text-blue-600 transition-colors" title="View Details">
                          <Eye size={15}/>
                        </button>
                        <button onClick={() => { setAssignModalApp(app); setSelectedWorker(''); }}
                          className="p-1.5 hover:bg-indigo-50 rounded-lg text-indigo-600 transition-colors" title="Assign Worker">
                          <UserCheck size={15}/>
                        </button>
                        <button onClick={() => { setStatusModalApp(app); setNewStatus(app.status); setRemarks(''); }}
                          className="px-2.5 py-1 bg-[#0F1B35] hover:bg-[#1E3A5F] text-white rounded-lg text-xs font-medium transition-colors whitespace-nowrap">
                          Status
                        </button>
                        <button onClick={() => setDeleteModalApp(app)}
                          className="p-1.5 hover:bg-red-50 rounded-lg text-red-400 hover:text-red-600 transition-colors" title="Delete Application">
                          <Trash2 size={15}/>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-5 py-4 border-t border-gray-100 flex items-center justify-between">
            <p className="text-xs text-gray-500">
              Page {page + 1} of {totalPages} Â· {totalElements} results
            </p>
            <div className="flex gap-2">
              <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
                className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50 transition-colors font-medium">
                â† Prev
              </button>
              <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
                className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50 transition-colors font-medium">
                Next â†’
              </button>
            </div>
          </div>
        )}
      </div>

      {/* â”€â”€ Assign Worker Modal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      <Modal isOpen={!!assignModalApp} onClose={() => setAssignModalApp(null)} title="Assign Worker">
        <p className="text-sm text-gray-500 mb-4">
          Assigning to: <span className="font-semibold text-gray-800">{assignModalApp?.applicationNumber}</span>
        </p>
        <select value={selectedWorker} onChange={e => setSelectedWorker(e.target.value)}
          className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm mb-5 focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
          <option value="">Select Worker</option>
          {(workers || []).map(w => (
            <option key={w.id} value={w.id}>
              {w.user?.firstName} {w.user?.lastName} â€” {w.department}
            </option>
          ))}
        </select>
        <div className="flex justify-end gap-3">
          <button onClick={() => setAssignModalApp(null)} className="px-5 py-2.5 rounded-xl border border-gray-200 text-sm font-medium hover:bg-gray-50 transition-colors">Cancel</button>
          <button onClick={handleAssign} disabled={actionLoading}
            className="px-5 py-2.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white text-sm font-semibold disabled:opacity-60 transition-all shadow-md">
            {actionLoading ? 'Assigning...' : 'Assign'}
          </button>
        </div>
      </Modal>

      {/* â”€â”€ Status Update Modal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      <Modal isOpen={!!statusModalApp} onClose={() => setStatusModalApp(null)} title="Update Application Status">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1.5">New Status</label>
            <select value={newStatus} onChange={e => setNewStatus(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
              <option value="">Select Status</option>
              {ALL_STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1.5">Remarks (optional)</label>
            <textarea rows={3} value={remarks} onChange={e => setRemarks(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none resize-none"
              placeholder="Add a note about this status change..."/>
          </div>
          <div className="flex justify-end gap-3 pt-1">
            <button onClick={() => setStatusModalApp(null)} className="px-5 py-2.5 rounded-xl border border-gray-200 text-sm font-medium hover:bg-gray-50 transition-colors">Cancel</button>
            <button onClick={handleUpdateStatus} disabled={actionLoading}
              className="px-5 py-2.5 rounded-xl bg-[#0F1B35] hover:bg-[#1E3A5F] text-white text-sm font-semibold disabled:opacity-60 transition-all shadow-md">
              {actionLoading ? 'Updating...' : 'Update Status'}
            </button>
          </div>
        </div>
      </Modal>

      {/* â”€â”€ Delete Confirmation Modal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      <Modal isOpen={!!deleteModalApp} onClose={() => setDeleteModalApp(null)} title="Delete Application">
        <div className="flex items-start gap-3 mb-5">
          <div className="shrink-0 w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
            <AlertTriangle size={18} className="text-red-600"/>
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-900 mb-1">
              Delete <span className="text-red-600 font-mono">{deleteModalApp?.applicationNumber}</span>?
            </p>
            <p className="text-sm text-gray-500">
              Customer: <span className="font-medium text-gray-700">{deleteModalApp?.customerName}</span>
              {deleteModalApp?.customerPhone && <span className="text-gray-400"> Â· {deleteModalApp.customerPhone}</span>}
            </p>
            {deleteModalApp?.modelName && (
              <p className="text-sm text-gray-500 mt-0.5">
                Bike: <span className="font-medium text-gray-700">{deleteModalApp.manufacturerName} {deleteModalApp.modelName}</span>
              </p>
            )}
            <p className="text-xs text-gray-400 mt-3 leading-relaxed">
              This will permanently delete the application and all its linked records
              (bike details, finance details, documents, status history).
              <strong className="text-gray-600"> Customer and bike master data will NOT be deleted.</strong>
              {deleteModalApp?.bikeSaleStatus === 'SOLD' || deleteModalApp?.bikeSaleStatus === 'RESERVED'
                ? ' The reserved/sold bike inventory will be reset to AVAILABLE.'
                : ''}
            </p>
          </div>
        </div>
        <div className="flex justify-end gap-3">
          <button onClick={() => setDeleteModalApp(null)}
            className="px-5 py-2.5 rounded-xl border border-gray-200 text-sm font-medium hover:bg-gray-50 transition-colors">
            Cancel
          </button>
          <button onClick={handleDelete} disabled={actionLoading}
            className="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-semibold disabled:opacity-60 transition-all shadow-md flex items-center gap-2">
            <Trash2 size={14}/>
            {actionLoading ? 'Deleting...' : 'Delete Application'}
          </button>
        </div>
      </Modal>
    </div>
  );
};

export default ApplicationsPage;
