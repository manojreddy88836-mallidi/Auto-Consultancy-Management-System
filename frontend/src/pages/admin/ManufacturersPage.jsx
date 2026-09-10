import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Pencil, ToggleLeft, ToggleRight, Building2, Search, RefreshCw, ChevronLeft, ChevronRight } from 'lucide-react';
import { toast } from 'react-hot-toast';
import api from '../../api/axios';

const COUNTRIES = [
  'India', 'Japan', 'Germany', 'United States', 'United Kingdom', 'Italy',
  'Austria', 'China', 'Sweden', 'Czech Republic', 'Vietnam', 'Taiwan', 'Other'
];

const Modal = ({ isOpen, onClose, title, children }) => {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 p-6 z-10">
        <h3 className="text-lg font-bold text-gray-900 mb-5">{title}</h3>
        {children}
      </div>
    </div>
  );
};

const StatusBadge = ({ active }) => (
  <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
    active ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'
  }`}>
    <span className={`w-1.5 h-1.5 rounded-full ${active ? 'bg-green-500' : 'bg-red-500'}`} />
    {active ? 'Active' : 'Inactive'}
  </span>
);

const PAGE_SIZE = 10;

const ManufacturersPage = () => {
  const navigate = useNavigate();
  const [items, setItems]           = useState([]);
  const [loading, setLoading]       = useState(true);
  const [search, setSearch]         = useState('');
  const [page, setPage]             = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [isModalOpen, setIsModalOpen]   = useState(false);
  const [editingItem, setEditingItem]   = useState(null);
  const [saving, setSaving]             = useState(false);
  const [formData, setFormData]         = useState({ name: '', country: 'India' });

  const fetchItems = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get('/manufacturers', { params: { page, size: PAGE_SIZE } });
      const d = res.data?.data;
      if (d?.content !== undefined) {
        setItems(d.content);
        setTotalPages(d.totalPages || 1);
        setTotalElements(d.totalElements || 0);
      } else {
        const arr = Array.isArray(d) ? d : [];
        setItems(arr);
        setTotalPages(1);
        setTotalElements(arr.length);
      }
    } catch {
      toast.error('Failed to load manufacturers');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { fetchItems(); }, [fetchItems]);

  const openAdd = () => {
    setEditingItem(null);
    setFormData({ name: '', country: 'India' });
    setIsModalOpen(true);
  };

  const openEdit = (item) => {
    setEditingItem(item);
    setFormData({ name: item.name, country: item.country || 'India' });
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) { toast.error('Manufacturer name is required'); return; }
    setSaving(true);
    try {
      if (editingItem) {
        await api.put(`/manufacturers/${editingItem.id}`, formData);
        toast.success('Manufacturer updated successfully');
      } else {
        await api.post('/manufacturers', formData);
        toast.success('Manufacturer added successfully');
      }
      setIsModalOpen(false);
      fetchItems();
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Operation failed');
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (item) => {
    try {
      await api.delete(`/manufacturers/${item.id}`);
      toast.success(`${item.name} ${item.active ? 'deactivated' : 'activated'} successfully`);
      fetchItems();
    } catch {
      toast.error('Failed to update manufacturer status');
    }
  };

  // Client-side search filter on current page
  const filtered = items.filter(m =>
    !search.trim() ||
    m.name?.toLowerCase().includes(search.toLowerCase()) ||
    m.country?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate(-1)}
            className="p-2 hover:bg-gray-100 rounded-xl transition-colors" title="Go back">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
              className="text-gray-500"><path d="m15 18-6-6 6-6"/></svg>
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
              <Building2 className="text-[#1E88E5]" size={24} /> Manufacturers
            </h1>
            <p className="text-gray-500 text-sm mt-1">{totalElements} manufacturers configured</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={fetchItems} className="p-2 hover:bg-gray-100 rounded-lg transition-colors" title="Refresh">
            <RefreshCw size={18} className="text-gray-500" />
          </button>
          <button
            onClick={openAdd}
            className="flex items-center gap-2 px-4 py-2.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-xl font-semibold text-sm transition-all shadow-md hover:shadow-lg"
          >
            <Plus size={18} /> Add Manufacturer
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
        <input
          value={search} onChange={e => setSearch(e.target.value)}
          placeholder="Search by name or country..."
          className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none text-sm"
        />
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-gray-400">
            <Building2 size={40} className="mb-3 opacity-30" />
            <p className="text-sm font-medium">{search ? 'No manufacturers match your search' : 'No manufacturers yet'}</p>
            {!search && <button onClick={openAdd} className="mt-3 text-[#1E88E5] text-sm hover:underline">Add one now</button>}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">#</th>
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">Manufacturer</th>
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">Country</th>
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">Status</th>
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">Added</th>
                  <th className="text-right px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map((m, i) => (
                  <tr key={m.id} className="hover:bg-gray-50/70 transition-colors">
                    <td className="px-6 py-4 text-gray-400 text-xs font-mono">{page * PAGE_SIZE + i + 1}</td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-100 to-blue-200 flex items-center justify-center text-xs font-bold text-[#1E88E5] uppercase">
                          {m.name?.[0]}
                        </div>
                        <span className="font-semibold text-gray-900">{m.name}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-gray-600">{m.country || '—'}</td>
                    <td className="px-6 py-4"><StatusBadge active={m.active} /></td>
                    <td className="px-6 py-4 text-gray-400 text-xs">
                      {m.createdAt ? new Date(m.createdAt).toLocaleDateString('en-IN') : '—'}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => openEdit(m)}
                          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        >
                          <Pencil size={12} /> Edit
                        </button>
                        <button
                          onClick={() => handleToggle(m)}
                          className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
                            m.active ? 'text-red-500 hover:bg-red-50' : 'text-green-600 hover:bg-green-50'
                          }`}
                        >
                          {m.active ? <ToggleLeft size={14} /> : <ToggleRight size={14} />}
                          {m.active ? 'Deactivate' : 'Activate'}
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
          <div className="px-6 py-4 border-t border-gray-100 flex items-center justify-between">
            <p className="text-xs text-gray-500">
              Page {page + 1} of {totalPages} · {totalElements} total
            </p>
            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
                className="flex items-center gap-1 px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50 transition-colors font-medium"
              >
                <ChevronLeft size={14} /> Prev
              </button>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => p + 1)}
                className="flex items-center gap-1 px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50 transition-colors font-medium"
              >
                Next <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Add / Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingItem ? 'Edit Manufacturer' : 'Add Manufacturer'}
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1.5">
              Manufacturer Name <span className="text-red-500">*</span>
            </label>
            <input
              required type="text" value={formData.name}
              onChange={e => setFormData({ ...formData, name: e.target.value })}
              className="w-full px-4 py-2.5 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none text-sm"
              placeholder="e.g. Royal Enfield"
            />
          </div>
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1.5">Country of Origin</label>
            <select
              value={formData.country}
              onChange={e => setFormData({ ...formData, country: e.target.value })}
              className="w-full px-4 py-2.5 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none text-sm bg-white"
            >
              {COUNTRIES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setIsModalOpen(false)}
              className="px-5 py-2.5 rounded-xl border border-gray-200 text-gray-700 font-medium text-sm hover:bg-gray-50 transition-colors">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="px-5 py-2.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white font-semibold text-sm transition-all disabled:opacity-60 shadow-md">
              {saving ? 'Saving...' : editingItem ? 'Update' : 'Add Manufacturer'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default ManufacturersPage;
