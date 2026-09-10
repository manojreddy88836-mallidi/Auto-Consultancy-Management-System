import React, { useState, useEffect, useCallback } from 'react';
import {
  Plus, Pencil, ChevronDown, ChevronUp, Search,
  RefreshCw, Bike, X, Info, Package, CheckCircle2, XCircle,
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import {
  getBikeModelsAdmin, createBikeModel, updateBikeModel,
  addVariant, deleteVariant, addManufacturingYear, deleteManufacturingYear,
} from '../../api/bikeModelApi';
import { getAllBrands } from '../../api/brandApi';

const CATEGORIES = ['Commuter','Sports','Cruiser','Scooter','Off-Road','Electric','Adventure'];
const FUEL_TYPES  = ['Petrol','Diesel','Electric','CNG','Hybrid'];
const PAGE_SIZE   = 20;

// ── Modal ────────────────────────────────────────────────────────────────────
const Modal = ({ isOpen, onClose, title, children }) => !isOpen ? null : (
  <div className="fixed inset-0 z-50 flex items-center justify-center">
    <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose}/>
    <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 p-6 z-10 max-h-[90vh] overflow-y-auto">
      <div className="flex items-center justify-between mb-5">
        <h3 className="text-lg font-bold text-gray-900">{title}</h3>
        <button onClick={onClose} className="p-1 hover:bg-gray-100 rounded-lg"><X size={18} className="text-gray-500"/></button>
      </div>
      {children}
    </div>
  </div>
);

// ── Inventory-derived status badge (READ-ONLY) ───────────────────────────────
/**
 * active is TRUE  when availableInventoryCount > 0
 * active is FALSE when there are no AVAILABLE bikes for this model
 *
 * This status is NEVER manually set — it comes from the Bikes/Inventory module.
 */
const InventoryStatusBadge = ({ active, availableInventoryCount }) => {
  const count   = availableInventoryCount ?? 0;
  const isActive = active === true;

  return (
    <div className="flex items-center gap-1.5" title={
      isActive
        ? `Active — ${count} bike${count !== 1 ? 's' : ''} available in inventory`
        : 'Inactive — no available bikes in inventory for this model'
    }>
      {isActive ? (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-green-100 text-green-700">
          <CheckCircle2 size={11}/>
          Active
          {count > 0 && <span className="ml-0.5 opacity-70">({count})</span>}
        </span>
      ) : (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-100 text-red-500">
          <XCircle size={11}/>
          Inactive
        </span>
      )}
    </div>
  );
};

// ── Page ─────────────────────────────────────────────────────────────────────
const BikeModelsPage = () => {
  const [models, setModels]         = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [manufacturers, setMfrs]    = useState([]);
  const [loading, setLoading]       = useState(true);
  const [search, setSearch]         = useState('');
  const [mfrFilter, setMfrFilter]   = useState('');
  const [statusFilter, setStatusFilter] = useState(''); // '' | 'active' | 'inactive'
  const [page, setPage]             = useState(0);
  const [expanded, setExpanded]     = useState({});

  // Add / Edit model modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editModel, setEditModel] = useState(null);
  const [saving, setSaving]       = useState(false);
  const [form, setForm]           = useState({
    manufacturerId: '', modelName: '', category: '', fuelType: '',
  });

  // Variant / year inline add
  const [variantForms, setVariantForms] = useState({});
  const [yearForms, setYearForms]       = useState({});

  // ── Fetch ──────────────────────────────────────────────────────────────────
  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const [modRes, mfrRes] = await Promise.all([
        getBikeModelsAdmin({ size: 1000, page: 0 }),
        getAllBrands(),
      ]);
      const d       = modRes.data?.data;
      const content = Array.isArray(d) ? d : d?.content || [];
      setModels(content);
      setTotalCount(d?.totalElements ?? content.length);
      setMfrs(mfrRes.data?.data || []);
    } catch { toast.error('Failed to load bike models'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);
  useEffect(() => { setPage(0); }, [search, mfrFilter, statusFilter]);

  // ── Model CRUD ─────────────────────────────────────────────────────────────
  const openAdd = () => {
    setEditModel(null);
    setForm({ manufacturerId: '', modelName: '', category: '', fuelType: '' });
    setModalOpen(true);
  };

  const openEdit = (m) => {
    setEditModel(m);
    setForm({
      manufacturerId: m.manufacturerId || '',
      modelName:      m.modelName      || '',
      category:       m.category       || '',
      fuelType:       m.fuelType       || '',
    });
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!form.manufacturerId || !form.modelName) {
      toast.error('Brand and model name required');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        manufacturerId: Number(form.manufacturerId),
        modelName:      form.modelName,
        category:       form.category,
        fuelType:       form.fuelType,
        // Do NOT include sale status / availableForSale — those are inventory-derived
      };
      if (editModel) await updateBikeModel(editModel.id, { ...editModel, ...payload });
      else           await createBikeModel(payload);
      toast.success(editModel ? 'Model updated' : 'Model created');
      setModalOpen(false);
      fetchAll();
    } catch (err) { toast.error(err?.response?.data?.message || 'Save failed'); }
    finally { setSaving(false); }
  };

  // ── Variants / Years ───────────────────────────────────────────────────────
  const handleAddVariant = async (modelId) => {
    const vf = variantForms[modelId] || {};
    if (!vf.variantName) { toast.error('Variant name required'); return; }
    try {
      await addVariant(modelId, { variantName: vf.variantName, engineCC: vf.engineCC || null });
      toast.success('Variant added');
      setVariantForms(f => ({ ...f, [modelId]: {} }));
      fetchAll();
    } catch { toast.error('Failed to add variant'); }
  };

  const handleDeleteVariant = async (variantId) => {
    try { await deleteVariant(variantId); toast.success('Variant removed'); fetchAll(); }
    catch { toast.error('Failed to remove variant'); }
  };

  const handleAddYear = async (modelId) => {
    const yf = yearForms[modelId] || {};
    if (!yf.year) { toast.error('Year required'); return; }
    try {
      await addManufacturingYear(modelId, { year: parseInt(yf.year) });
      toast.success('Year added');
      setYearForms(f => ({ ...f, [modelId]: {} }));
      fetchAll();
    } catch { toast.error('Failed to add year'); }
  };

  const handleDeleteYear = async (yearId) => {
    try { await deleteManufacturingYear(yearId); toast.success('Year removed'); fetchAll(); }
    catch { toast.error('Failed to remove year'); }
  };

  // ── Filter / paginate ──────────────────────────────────────────────────────
  const filtered = models.filter(m => {
    const q        = search.toLowerCase();
    const matchQ   = !q || m.modelName?.toLowerCase().includes(q) || m.manufacturerName?.toLowerCase().includes(q);
    const matchMfr = !mfrFilter || String(m.manufacturerId) === mfrFilter;
    const isActive = m.active === true;
    const matchSt  = !statusFilter
      || (statusFilter === 'active'   &&  isActive)
      || (statusFilter === 'inactive' && !isActive);
    return matchQ && matchMfr && matchSt;
  });
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paginated  = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  const inputCls = "w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none";

  // ── Summary stats ──────────────────────────────────────────────────────────
  const activeCount   = models.filter(m => m.active === true).length;
  const inactiveCount = models.length - activeCount;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Bike className="text-[#1E88E5]" size={24}/> Bike Models
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            {loading ? 'Loading…' : (search || mfrFilter || statusFilter
              ? `${filtered.length} of ${totalCount} models`
              : `${totalCount} models total`)}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={fetchAll} className="p-2 hover:bg-gray-100 rounded-lg transition-colors" title="Refresh">
            <RefreshCw size={18} className="text-gray-500"/>
          </button>
          <button onClick={openAdd}
            className="flex items-center gap-2 px-4 py-2.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-xl font-semibold text-sm transition-all shadow-md hover:shadow-lg">
            <Plus size={18}/> Add Model
          </button>
        </div>
      </div>

      {/* Info banner */}
      <div className="flex items-start gap-3 p-3 bg-blue-50 border border-blue-100 rounded-xl text-sm text-blue-700">
        <Info size={16} className="flex-shrink-0 mt-0.5" />
        <span>
          <strong>Active/Inactive</strong> status is <strong>automatically calculated</strong> from the{' '}
          <strong>Bikes</strong> inventory module — it cannot be manually changed here.
          A model is <strong className="text-green-700">Active</strong> only when it has at least one{' '}
          <strong>AVAILABLE</strong> bike in inventory. Go to <strong>Bikes</strong> in the sidebar to manage stock.
        </span>
      </div>

      {/* Summary cards */}
      {!loading && (
        <div className="grid grid-cols-3 gap-3">
          <div className="bg-white rounded-xl border border-gray-100 p-3 flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center">
              <Bike size={16} className="text-gray-500"/>
            </div>
            <div>
              <p className="text-xs text-gray-400">Total Models</p>
              <p className="text-lg font-bold text-gray-900">{models.length}</p>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-100 p-3 flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-green-100 flex items-center justify-center">
              <CheckCircle2 size={16} className="text-green-600"/>
            </div>
            <div>
              <p className="text-xs text-gray-400">Active (has stock)</p>
              <p className="text-lg font-bold text-green-700">{activeCount}</p>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-100 p-3 flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-red-100 flex items-center justify-center">
              <XCircle size={16} className="text-red-500"/>
            </div>
            <div>
              <p className="text-xs text-gray-400">Inactive (no stock)</p>
              <p className="text-lg font-bold text-red-500">{inactiveCount}</p>
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 flex-wrap">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search models..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
        </div>
        <select value={mfrFilter} onChange={e => setMfrFilter(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
          <option value="">All Brands</option>
          {manufacturers.map(m => <option key={m.id} value={String(m.id)}>{m.name}</option>)}
        </select>
        <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
          <option value="">All Status</option>
          <option value="active">Active (has stock)</option>
          <option value="inactive">Inactive (no stock)</option>
        </select>
      </div>

      {/* List */}
      {loading ? (
        <div className="space-y-3">{[1,2,3].map(i => <div key={i} className="bg-white rounded-2xl h-20 animate-pulse border border-gray-100"/>)}</div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400 bg-white rounded-2xl border border-gray-100">
          <Bike size={40} className="mb-3 opacity-25"/>
          <p className="text-sm font-medium">No bike models found</p>
          <button onClick={openAdd} className="mt-3 text-[#1E88E5] text-sm hover:underline">Add first model</button>
        </div>
      ) : (
        <div className="space-y-3">
          {paginated.map(model => {
            const isOpen = expanded[model.id];
            return (
              <div key={model.id} className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
                {/* Model row */}
                <div className="flex items-center justify-between p-4 gap-3">
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div className="w-10 h-10 rounded-xl bg-gray-100 flex items-center justify-center flex-shrink-0">
                      <Bike size={18} className="text-gray-400"/>
                    </div>
                    <div className="min-w-0">
                      <p className="font-bold text-gray-900 truncate">{model.modelName}</p>
                      <p className="text-xs text-gray-400 mt-0.5 truncate">
                        {model.manufacturerName} · {model.category || '—'} · {model.fuelType || '—'}
                      </p>
                    </div>
                  </div>

                  {/* Right side — status badge + edit + expand */}
                  <div className="flex items-center gap-1.5 flex-shrink-0">
                    {/* Inventory-derived status badge (READ-ONLY — no toggle) */}
                    <InventoryStatusBadge
                      active={model.active}
                      availableInventoryCount={model.availableInventoryCount}
                    />

                    {/* Inventory count pill (if > 0) */}
                    {(model.availableInventoryCount ?? 0) > 0 && (
                      <span className="hidden sm:inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs text-gray-500 bg-gray-100">
                        <Package size={10}/> {model.availableInventoryCount} in stock
                      </span>
                    )}

                    <button onClick={() => openEdit(model)}
                      className="p-1.5 hover:bg-blue-50 rounded-lg transition-colors" title="Edit catalog details">
                      <Pencil size={14} className="text-blue-600"/>
                    </button>

                    <button onClick={() => setExpanded(p => ({ ...p, [model.id]: !p[model.id] }))}
                      className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors">
                      {isOpen ? <ChevronUp size={16} className="text-gray-400"/> : <ChevronDown size={16} className="text-gray-400"/>}
                    </button>
                  </div>
                </div>

                {/* Expanded: Variants + Years */}
                {isOpen && (
                  <div className="border-t border-gray-100 grid sm:grid-cols-2 divide-y sm:divide-y-0 sm:divide-x divide-gray-100">
                    {/* Variants */}
                    <div className="p-4">
                      <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Variants ({model.variants?.length || 0})</p>
                      <div className="space-y-1.5 mb-3">
                        {(model.variants || []).map(v => (
                          <div key={v.id} className="flex items-center justify-between text-sm bg-gray-50 rounded-lg px-3 py-1.5">
                            <span className="font-medium text-gray-800">{v.variantName}</span>
                            <div className="flex items-center gap-2">
                              {v.engineCC && <span className="text-xs text-gray-400">{v.engineCC}cc</span>}
                              <button onClick={() => handleDeleteVariant(v.id)}
                                className="p-0.5 hover:bg-red-100 rounded text-red-400 hover:text-red-600 transition-colors"><X size={13}/></button>
                            </div>
                          </div>
                        ))}
                        {(!model.variants || model.variants.length === 0) && <p className="text-xs text-gray-300 italic">No variants</p>}
                      </div>
                      <div className="flex gap-2">
                        <input value={variantForms[model.id]?.variantName || ''} placeholder="Variant name"
                          onChange={e => setVariantForms(f => ({ ...f, [model.id]: { ...f[model.id], variantName: e.target.value }}))}
                          className="flex-1 px-3 py-1.5 rounded-lg border border-gray-200 text-xs focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
                        <input value={variantForms[model.id]?.engineCC || ''} placeholder="CC" type="number"
                          onChange={e => setVariantForms(f => ({ ...f, [model.id]: { ...f[model.id], engineCC: e.target.value }}))}
                          className="w-16 px-2 py-1.5 rounded-lg border border-gray-200 text-xs focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
                        <button onClick={() => handleAddVariant(model.id)}
                          className="px-3 py-1.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white text-xs font-semibold rounded-lg transition-all"><Plus size={13}/></button>
                      </div>
                    </div>

                    {/* Manufacturing Years */}
                    <div className="p-4">
                      <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Mfg. Years ({model.years?.length || 0})</p>
                      <div className="flex flex-wrap gap-1.5 mb-3">
                        {(model.years || []).sort((a,b) => b - a).map(y => (
                          <div key={y} className="flex items-center gap-1 bg-gray-50 border border-gray-200 rounded-lg px-2.5 py-1">
                            <span className="text-xs font-semibold text-gray-700">{y}</span>
                          </div>
                        ))}
                        {(!model.years || model.years.length === 0) && <p className="text-xs text-gray-300 italic">No years</p>}
                      </div>
                      <div className="flex gap-2">
                        <input value={yearForms[model.id]?.year || ''} placeholder="e.g. 2023" type="number" min="1990" max="2030"
                          onChange={e => setYearForms(f => ({ ...f, [model.id]: { year: e.target.value }}))}
                          className="flex-1 px-3 py-1.5 rounded-lg border border-gray-200 text-xs focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
                        <button onClick={() => handleAddYear(model.id)}
                          className="px-3 py-1.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white text-xs font-semibold rounded-lg transition-all"><Plus size={13}/></button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {!loading && totalPages > 1 && (
        <div className="flex items-center justify-between pt-2">
          <p className="text-sm text-gray-500">
            Showing {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, filtered.length)} of {filtered.length}
          </p>
          <div className="flex items-center gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
              className="px-3 py-1.5 rounded-lg border border-gray-200 text-sm font-medium hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">← Prev</button>
            <span className="text-sm text-gray-600 font-medium px-2">Page {page + 1} / {totalPages}</span>
            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
              className="px-3 py-1.5 rounded-lg border border-gray-200 text-sm font-medium hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">Next →</button>
          </div>
        </div>
      )}

      {/* Add / Edit Modal — catalog fields only */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editModel ? 'Edit Bike Model' : 'Add New Bike Model'}>
        <form onSubmit={handleSave} className="space-y-4">
          {/* Info note inside modal */}
          <div className="flex items-start gap-2 p-2.5 bg-amber-50 border border-amber-100 rounded-xl text-xs text-amber-700">
            <Info size={13} className="flex-shrink-0 mt-0.5"/>
            <span>
              The <strong>Active/Inactive</strong> status is automatically calculated from the{' '}
              <strong>Bikes</strong> module inventory — you cannot set it here.
            </span>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1.5">Brand *</label>
            <select required value={form.manufacturerId} onChange={e => setForm({...form, manufacturerId: e.target.value})}
              className={inputCls + " bg-white"}>
              <option value="">Select brand</option>
              {manufacturers.filter(m => m.active !== false).map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1.5">Model Name *</label>
            <input required value={form.modelName} onChange={e => setForm({...form, modelName: e.target.value})}
              placeholder="e.g. Splendor Plus" className={inputCls}/>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Category</label>
              <select value={form.category} onChange={e => setForm({...form, category: e.target.value})} className={inputCls + " bg-white"}>
                <option value="">Select</option>
                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Fuel Type</label>
              <select value={form.fuelType} onChange={e => setForm({...form, fuelType: e.target.value})} className={inputCls + " bg-white"}>
                <option value="">Select</option>
                {FUEL_TYPES.map(f => <option key={f} value={f}>{f}</option>)}
              </select>
            </div>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)}
              className="px-5 py-2.5 rounded-xl border border-gray-200 text-sm font-medium hover:bg-gray-50 transition-colors">Cancel</button>
            <button type="submit" disabled={saving}
              className="px-5 py-2.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white text-sm font-semibold transition-all disabled:opacity-60 shadow-md">
              {saving ? 'Saving…' : editModel ? 'Update Model' : 'Create Model'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default BikeModelsPage;
