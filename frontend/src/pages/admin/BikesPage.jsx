import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Bike, Plus, Search, RefreshCw, Image, Trash2, Edit2, X,
  CheckCircle, Tag, UploadCloud, Star, Eye, AlertCircle,
  ChevronLeft, ChevronRight, ZoomIn, Filter, IndianRupee,
  Calendar, Gauge, Palette, Fuel, PackageX
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import {
  getBikeInventory, createBikeInventory, updateBikeInventory, deleteBikeInventory,
  enableSale, disableSale, reserveBike, markSold,
  uploadBikeInventoryImage, getBikeInventoryImages,
  setPrimaryBikeInventoryImage, deleteBikeInventoryImage
} from '../../api/bikeInventoryApi';
import { getBikeModelsAdmin } from '../../api/bikeModelApi';
import { getAllBrands } from '../../api/brandApi';

const BACKEND_BASE = 'http://localhost:8080';
const PAGE_SIZE = 12;

const SALE_STATUS_META = {
  AVAILABLE:    { label: 'Available',    cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
  RESERVED:     { label: 'Reserved',     cls: 'bg-amber-100 text-amber-700 border-amber-200' },
  SOLD:         { label: 'Sold',         cls: 'bg-red-100 text-red-700 border-red-200' },
  NOT_FOR_SALE: { label: 'Not for Sale', cls: 'bg-gray-100 text-gray-500 border-gray-200' },
};

const CONDITIONS = ['Excellent', 'Good', 'Fair', 'Poor'];
const FUEL_TYPES  = ['Petrol', 'Diesel', 'Electric', 'CNG', 'Hybrid'];

export default function BikesPage() {
  const [bikes, setBikes]                   = useState([]);
  const [totalCount, setTotalCount]         = useState(0);
  const [totalPages, setTotalPages]         = useState(0);
  const [brands, setBrands]                 = useState([]);
  const [models, setModels]                 = useState([]);
  const [loading, setLoading]               = useState(false);

  // Filters
  const [search, setSearch]                 = useState('');
  const [brandFilter, setBrandFilter]       = useState('');
  const [saleFilter, setSaleFilter]         = useState('');
  const [page, setPage]                     = useState(0);

  // Add/Edit modal
  const [modalOpen, setModalOpen]           = useState(false);
  const [editBike, setEditBike]             = useState(null);
  const [saving, setSaving]                 = useState(false);
  const [form, setForm]                     = useState({
    bikeModelId: '', bikeCode: '', registrationNumber: '',
    price: '', year: '', color: '', kmDriven: '',
    fuelType: 'Petrol', conditionType: 'Good', description: ''
  });

  // Image manager modal
  const [imgModalOpen, setImgModalOpen]     = useState(false);
  const [imgBike, setImgBike]               = useState(null);
  const [images, setImages]                 = useState([]);
  const [imgLoading, setImgLoading]         = useState(false);
  const [uploading, setUploading]           = useState(false);
  const [dragOver, setDragOver]             = useState(false);
  const fileInputRef                        = useRef();

  // Lightbox
  const [lightbox, setLightbox]             = useState({ open: false, idx: 0 });

  // â”€â”€â”€ Data fetching â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: PAGE_SIZE };
      if (search) params.search = search;
      if (brandFilter) params.manufacturerId = brandFilter;
      if (saleFilter) params.saleStatus = saleFilter;

      const [invRes, brandRes] = await Promise.allSettled([
        getBikeInventory(params),
        getAllBrands()
      ]);

      if (invRes.status === 'fulfilled') {
        const d = invRes.value.data.data;
        setBikes(d.content || []);
        setTotalCount(d.totalElements || 0);
        setTotalPages(d.totalPages || 0);
      } else {
        const status = invRes.reason?.response?.status;
        if (status === 403) toast.error('Access denied. Please log in as Admin or Worker.');
        else toast.error('Failed to load bike inventory.');
      }
      if (brandRes.status === 'fulfilled') setBrands(brandRes.value.data.data || []);
    } catch (e) {
      console.error('Failed to fetch bike inventory', e);
    } finally {
      setLoading(false);
    }
  }, [page, search, brandFilter, saleFilter]);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  // Fetch models when brand changes in modal
  useEffect(() => {
    if (!modalOpen) return;
    if (!form.bikeModelId && !brandFilter) {
      getBikeModelsAdmin({ size: 1000 })
        .then(r => setModels(r.data.data?.content || []))
        .catch(() => {});
    }
  }, [modalOpen]);

  const fetchModelsForBrand = async (brandId) => {
    try {
      const res = await getBikeModelsAdmin({ manufacturerId: brandId || undefined, size: 1000 });
      setModels(res.data.data?.content || []);
    } catch {}
  };

  // â”€â”€â”€ CRUD handlers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const openAdd = () => {
    setEditBike(null);
    setForm({ bikeModelId: '', bikeCode: '', registrationNumber: '', price: '',
              year: '', color: '', kmDriven: '', fuelType: 'Petrol', conditionType: 'Good', description: '' });
    fetchModelsForBrand('');
    setModalOpen(true);
  };

  const openEdit = (b) => {
    setEditBike(b);
    setForm({
      bikeModelId: b.bikeModelId || '', bikeCode: b.bikeCode || '',
      registrationNumber: b.registrationNumber || '', price: b.price || '',
      year: b.year || '', color: b.color || '', kmDriven: b.kmDriven || '',
      fuelType: b.fuelType || 'Petrol', conditionType: b.conditionType || 'Good',
      description: b.description || ''
    });
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!form.bikeModelId) { toast.error('Please select a Bike Model.'); return; }
    setSaving(true);
    try {
      const payload = {
        bikeModelId: Number(form.bikeModelId),
        bikeCode: form.bikeCode || null,
        registrationNumber: form.registrationNumber || null,
        price: form.price ? Number(form.price) : null,
        year: form.year ? Number(form.year) : null,
        color: form.color || null,
        kmDriven: form.kmDriven ? Number(form.kmDriven) : null,
        fuelType: form.fuelType,
        conditionType: form.conditionType,
        description: form.description || null,
      };
      if (editBike) await updateBikeInventory(editBike.id, payload);
      else          await createBikeInventory(payload);
      setModalOpen(false);
      fetchAll();
      toast.success(editBike ? 'Bike updated!' : 'Bike added to inventory!');
    } catch (err) {
      const msg = err?.response?.data?.message;
      toast.error(msg || 'Save failed. Please try again.');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this bike? This cannot be undone.')) return;
    try { await deleteBikeInventory(id); fetchAll(); toast.success('Bike deleted.'); }
    catch (err) {
      const msg = err?.response?.data?.message;
      const status = err?.response?.status;
      if (status === 403) toast.error('Access denied. Please re-login as Admin.');
      else toast.error(msg || 'Delete failed');
    }
  };

  const handleSaleAction = async (action, bikeId) => {
    try {
      const fns = { enable: enableSale, disable: disableSale, reserve: reserveBike, sold: markSold };
      await fns[action](bikeId);
      fetchAll();
      toast.success({
        enable: 'Bike marked as Available for Sale!',
        disable: 'Bike removed from sale.',
        reserve: 'Bike marked as Reserved.',
        sold: 'Bike marked as Sold.',
      }[action] || 'Done');
    } catch (err) {
      const status = err?.response?.status;
      const msg = err?.response?.data?.message;
      if (status === 400 && msg) toast.error(msg);
      else if (status === 403) toast.error('Access denied. Please re-login as Admin.');
      else toast.error(msg || 'Action failed. Please try again.');
    }
  };

  // â”€â”€â”€ Image Manager â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const openImgManager = async (bike) => {
    setImgBike(bike);
    setImgModalOpen(true);
    setImages([]);
    setImgLoading(true);
    try {
      const res = await getBikeInventoryImages(bike.id);
      setImages(res.data.data || []);
    } catch {} finally { setImgLoading(false); }
  };

  const refreshImages = async () => {
    if (!imgBike) return;
    setImgLoading(true);
    try {
      const res = await getBikeInventoryImages(imgBike.id);
      setImages(res.data.data || []);
    } catch {} finally { setImgLoading(false); }
    fetchAll(); // refresh list too (to update imageCount)
  };

  const handleFileUpload = async (files, setPrimary = false) => {
    const allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    const file = Array.from(files)[0];
    if (!file) return;
    if (!allowed.includes(file.type)) { toast.error('Only JPG, PNG, WEBP files allowed.'); return; }
    if (file.size > 10 * 1024 * 1024) { toast.error('File too large (max 10 MB).'); return; }
    setUploading(true);
    try {
      await uploadBikeInventoryImage(imgBike.id, file, images.length === 0 || setPrimary);
      await refreshImages();
      toast.success('Image uploaded!');
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Upload failed');
    } finally { setUploading(false); }
  };

  const handleMultiUpload = async (files) => {
    const allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    const validFiles = Array.from(files).filter(f => allowed.includes(f.type) && f.size <= 10 * 1024 * 1024);
    if (!validFiles.length) { toast.error('No valid files. Use JPG/PNG/WEBP under 10MB.'); return; }
    setUploading(true);
    try {
      for (let i = 0; i < validFiles.length; i++) {
        await uploadBikeInventoryImage(imgBike.id, validFiles[i], i === 0 && images.length === 0);
      }
      await refreshImages();
      toast.success(`${validFiles.length} image(s) uploaded!`);
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Upload failed');
    } finally { setUploading(false); }
  };

  const handleSetPrimary = async (imageId) => {
    try { await setPrimaryBikeInventoryImage(imgBike.id, imageId); await refreshImages(); toast.success('Primary image set!'); }
    catch (err) { toast.error(err?.response?.data?.message || 'Failed to set primary'); }
  };

  const handleDeleteImage = async (imageId) => {
    if (!window.confirm('Delete this image?')) return;
    try { await deleteBikeInventoryImage(imgBike.id, imageId); await refreshImages(); toast.success('Image deleted.'); }
    catch (err) { toast.error(err?.response?.data?.message || 'Failed to delete image'); }
  };

  // â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const imgSrc = (bike) =>
    bike.primaryImageUrl ? `${BACKEND_BASE}${bike.primaryImageUrl}` : null;

  const fmtPrice = (p) => p ? `â‚¹${Number(p).toLocaleString('en-IN')}` : 'â€”';

  // â”€â”€â”€ UI â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Bike className="text-blue-600" size={28} />
            Bikes
          </h1>
          <p className="text-sm text-gray-500 mt-0.5">Manage actual bikes available for sale</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-500">{totalCount} total</span>
          <button onClick={fetchAll} className="p-2 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors" title="Refresh">
            <RefreshCw size={16} className={loading ? 'animate-spin text-blue-500' : 'text-gray-500'} />
          </button>
          <button onClick={openAdd} className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors font-medium shadow-sm">
            <Plus size={16} /> Add Bike
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
        <div className="relative flex-1 min-w-[200px]">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(0); }}
            placeholder="Search by model, bike code..."
            className="pl-8 pr-3 py-2 border border-gray-200 rounded-lg text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-300" />
        </div>
        <select value={brandFilter} onChange={e => { setBrandFilter(e.target.value); setPage(0); }}
          className="border border-gray-200 rounded-lg text-sm px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-300 bg-white">
          <option value="">All Brands</option>
          {brands.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
        </select>
        <select value={saleFilter} onChange={e => { setSaleFilter(e.target.value); setPage(0); }}
          className="border border-gray-200 rounded-lg text-sm px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-300 bg-white">
          <option value="">All Statuses</option>
          {Object.entries(SALE_STATUS_META).map(([k, v]) =>
            <option key={k} value={k}>{v.label}</option>)}
        </select>
      </div>

      {/* Bike Grid */}
      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full" />
        </div>
      ) : (bikes || []).length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-gray-400 gap-3">
          <PackageX size={48} className="text-gray-300" />
          <p className="font-medium">No bikes found</p>
          <button onClick={openAdd} className="text-blue-600 text-sm hover:underline">Add your first bike</button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {(bikes || []).map(bike => <BikeCard key={bike.id} bike={bike} imgSrc={imgSrc}
            fmtPrice={fmtPrice} onEdit={openEdit} onDelete={handleDelete}
            onSaleAction={handleSaleAction} onImages={openImgManager} />)}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-gray-200 pt-4">
          <p className="text-sm text-gray-500">
            Page {page + 1} of {totalPages} Â· {totalCount} bikes
          </p>
          <div className="flex gap-2">
            <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
              className="p-2 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50 transition-colors">
              <ChevronLeft size={16} />
            </button>
            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
              className="p-2 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50 transition-colors">
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}

      {/* Add/Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between p-5 border-b border-gray-100">
              <h2 className="font-semibold text-gray-900">{editBike ? 'Edit Bike' : 'Add New Bike'}</h2>
              <button onClick={() => setModalOpen(false)} className="p-1.5 rounded-lg hover:bg-gray-100">
                <X size={18} className="text-gray-500" />
              </button>
            </div>
            <form onSubmit={handleSave} className="flex-1 overflow-y-auto p-5 space-y-4">
              {/* Bike Model selector */}
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-gray-600 mb-1">Bike Model <span className="text-red-500">*</span></label>
                  <select value={form.bikeModelId} required
                    onChange={e => setForm(f => ({ ...f, bikeModelId: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300">
                    <option value="">Select Bike Model</option>
                    {models.map(m => (
                      <option key={m.id} value={m.id}>
                        {m.manufacturerName} Â· {m.modelName}{m.category ? ` (${m.category})` : ''}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Bike Code / Inventory ID</label>
                  <input value={form.bikeCode} placeholder="e.g. RE-H350-001"
                    onChange={e => setForm(f => ({ ...f, bikeCode: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Registration Number</label>
                  <input value={form.registrationNumber} placeholder="e.g. MH-12-AB-1234"
                    onChange={e => setForm(f => ({ ...f, registrationNumber: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Price (â‚¹)</label>
                  <input type="number" value={form.price} placeholder="185000"
                    onChange={e => setForm(f => ({ ...f, price: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Year</label>
                  <input type="number" value={form.year} placeholder={new Date().getFullYear()}
                    onChange={e => setForm(f => ({ ...f, year: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Color</label>
                  <input value={form.color} placeholder="e.g. Stealth Black"
                    onChange={e => setForm(f => ({ ...f, color: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">KM Driven</label>
                  <input type="number" value={form.kmDriven} placeholder="4500"
                    onChange={e => setForm(f => ({ ...f, kmDriven: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Fuel Type</label>
                  <select value={form.fuelType} onChange={e => setForm(f => ({ ...f, fuelType: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300">
                    {FUEL_TYPES.map(t => <option key={t}>{t}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Condition</label>
                  <select value={form.conditionType} onChange={e => setForm(f => ({ ...f, conditionType: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300">
                    {CONDITIONS.map(c => <option key={c}>{c}</option>)}
                  </select>
                </div>
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-gray-600 mb-1">Description</label>
                  <textarea value={form.description} rows={3} placeholder="Bike details, features, notes..."
                    onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 resize-none" />
                </div>
              </div>
              {!editBike && (
                <div className="rounded-lg bg-amber-50 border border-amber-200 p-3 text-xs text-amber-700 flex gap-2">
                  <AlertCircle size={14} className="flex-shrink-0 mt-0.5" />
                  <span>After creating the bike, upload images to enable it for sale.</span>
                </div>
              )}
            </form>
            <div className="p-5 border-t border-gray-100 flex justify-end gap-3">
              <button type="button" onClick={() => setModalOpen(false)}
                className="px-4 py-2 text-sm border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors">Cancel</button>
              <button onClick={handleSave} disabled={saving}
                className="px-5 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium disabled:opacity-50">
                {saving ? 'Saving...' : editBike ? 'Update Bike' : 'Create Bike'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Image Manager Modal */}
      {imgModalOpen && imgBike && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between p-5 border-b border-gray-100">
              <div>
                <h2 className="font-semibold text-gray-900">Image Manager</h2>
                <p className="text-xs text-gray-500 mt-0.5">
                  {imgBike.manufacturerName} {imgBike.modelName}
                  {imgBike.bikeCode ? ` Â· ${imgBike.bikeCode}` : ''}
                  <span className="ml-2 font-medium">{images.length} image{images.length !== 1 ? 's' : ''}</span>
                </p>
              </div>
              <button onClick={() => { setImgModalOpen(false); setImages([]); fetchAll(); }}
                className="p-1.5 rounded-lg hover:bg-gray-100"><X size={18} className="text-gray-500" /></button>
            </div>

            <div className="flex-1 overflow-y-auto p-5 space-y-5">
              {/* Upload area */}
              <div
                className={`border-2 border-dashed rounded-xl p-6 text-center transition-colors cursor-pointer
                  ${dragOver ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-blue-400 hover:bg-gray-50'}`}
                onDragOver={e => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={e => { e.preventDefault(); setDragOver(false); handleMultiUpload(e.dataTransfer.files); }}
                onClick={() => fileInputRef.current?.click()}>
                <UploadCloud size={32} className="mx-auto text-gray-400 mb-2" />
                <p className="text-sm font-medium text-gray-600">
                  {uploading ? 'Uploading...' : 'Click or drag & drop to upload images'}
                </p>
                <p className="text-xs text-gray-400 mt-1">JPG, PNG, WEBP Â· Max 10MB each Â· Multiple files supported</p>
                <input ref={fileInputRef} type="file" accept="image/jpeg,image/png,image/webp"
                  multiple className="hidden"
                  onChange={e => handleMultiUpload(e.target.files)} />
              </div>

              {/* No-image warning */}
              {!imgLoading && images.length === 0 && (
                <div className="flex items-center gap-3 p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-700 text-sm">
                  <AlertCircle size={16} className="flex-shrink-0" />
                  <span>No images uploaded. Upload at least one image to enable this bike for sale.</span>
                </div>
              )}

              {/* Image grid */}
              {imgLoading ? (
                <div className="flex justify-center py-8">
                  <div className="animate-spin w-6 h-6 border-4 border-blue-500 border-t-transparent rounded-full" />
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {images.map((img, idx) => (
                    <div key={img.id} className="relative group rounded-xl overflow-hidden border-2 border-gray-200 aspect-video bg-gray-100">
                      <img
                        src={`${BACKEND_BASE}${img.imageUrl}`}
                        alt={img.originalFileName}
                        className="w-full h-full object-cover"
                        onError={e => { e.target.style.display = 'none'; }}
                      />
                      {img.primary && (
                        <div className="absolute top-2 left-2 flex items-center gap-1 bg-yellow-400 text-yellow-900 text-xs font-bold px-2 py-0.5 rounded-full shadow">
                          <Star size={10} fill="currentColor" /> Primary
                        </div>
                      )}
                      {/* Hover overlay */}
                      <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                        <button onClick={() => setLightbox({ open: true, idx })}
                          className="p-2 bg-white rounded-full text-gray-800 hover:bg-gray-100" title="View">
                          <ZoomIn size={14} />
                        </button>
                        {!img.primary && (
                          <button onClick={() => handleSetPrimary(img.id)}
                            className="p-2 bg-yellow-400 rounded-full text-yellow-900 hover:bg-yellow-300" title="Set primary">
                            <Star size={14} />
                          </button>
                        )}
                        <button onClick={() => handleDeleteImage(img.id)}
                          className="p-2 bg-red-500 rounded-full text-white hover:bg-red-600" title="Delete">
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Lightbox */}
      {lightbox.open && images.length > 0 && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/90 p-4"
          onClick={() => setLightbox(l => ({ ...l, open: false }))}>
          <button onClick={e => { e.stopPropagation(); setLightbox(l => ({ ...l, idx: (l.idx - 1 + images.length) % images.length })); }}
            className="absolute left-4 p-2 bg-white/20 rounded-full text-white hover:bg-white/30">
            <ChevronLeft size={24} />
          </button>
          <img
            src={`${BACKEND_BASE}${images[lightbox.idx]?.imageUrl}`}
            className="max-h-[85vh] max-w-[85vw] object-contain rounded-xl"
            onClick={e => e.stopPropagation()}
            alt="Full view"
          />
          <button onClick={e => { e.stopPropagation(); setLightbox(l => ({ ...l, idx: (l.idx + 1) % images.length })); }}
            className="absolute right-4 p-2 bg-white/20 rounded-full text-white hover:bg-white/30">
            <ChevronRight size={24} />
          </button>
          <button onClick={() => setLightbox(l => ({ ...l, open: false }))}
            className="absolute top-4 right-4 p-2 bg-white/20 rounded-full text-white hover:bg-white/30">
            <X size={20} />
          </button>
          <p className="absolute bottom-4 text-white/60 text-sm">
            {lightbox.idx + 1} / {images.length}
          </p>
        </div>
      )}
    </div>
  );
}

// â”€â”€â”€ Bike Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
function BikeCard({ bike, imgSrc, fmtPrice, onEdit, onDelete, onSaleAction, onImages }) {
  const [imgErr, setImgErr] = useState(false);
  const src = imgSrc(bike);
  const sm = SALE_STATUS_META[bike.saleStatus] || SALE_STATUS_META.NOT_FOR_SALE;
  const noImage = !src || imgErr;

  return (
    <div className="bg-white rounded-2xl border border-gray-200 shadow-sm hover:shadow-md transition-shadow overflow-hidden flex flex-col">
      {/* Image */}
      <div className="relative h-44 bg-gray-100 overflow-hidden">
        {noImage ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-300 gap-2">
            <Bike size={40} strokeWidth={1.5} />
            <span className="text-xs font-medium text-gray-400">No Image</span>
          </div>
        ) : (
          <img src={src} alt={bike.modelName} className="w-full h-full object-cover"
            onError={() => setImgErr(true)} />
        )}
        {/* Status badge overlay */}
        <span className={`absolute top-2 right-2 text-xs font-semibold px-2.5 py-1 rounded-full border ${sm.cls}`}>
          {sm.label}
        </span>
        {/* Image count */}
        <button onClick={() => onImages(bike)}
          className="absolute bottom-2 right-2 flex items-center gap-1 bg-black/60 text-white text-xs px-2 py-1 rounded-full hover:bg-black/80 transition-colors">
          <Image size={11} /> {bike.imageCount}
        </button>
      </div>

      {/* Details */}
      <div className="p-4 flex flex-col flex-1">
        <div className="flex-1 space-y-1">
          <p className="text-xs text-blue-600 font-semibold uppercase tracking-wide">{bike.manufacturerName}</p>
          <h3 className="font-bold text-gray-900 text-base leading-tight">{bike.modelName}</h3>
          {bike.bikeCode && (
            <p className="text-xs text-gray-500 font-mono">{bike.bikeCode}</p>
          )}
          <div className="flex flex-wrap gap-x-3 gap-y-1 mt-2 text-xs text-gray-500">
            {bike.price && <span className="flex items-center gap-1 font-semibold text-gray-800"><IndianRupee size={11}/>{Number(bike.price).toLocaleString('en-IN')}</span>}
            {bike.year && <span className="flex items-center gap-1"><Calendar size={11}/>{bike.year}</span>}
            {bike.kmDriven && <span className="flex items-center gap-1"><Gauge size={11}/>{Number(bike.kmDriven).toLocaleString('en-IN')} km</span>}
            {bike.color && <span className="flex items-center gap-1"><Palette size={11}/>{bike.color}</span>}
            {bike.fuelType && <span className="flex items-center gap-1"><Fuel size={11}/>{bike.fuelType}</span>}
          </div>
        </div>

        {/* No-image warning */}
        {bike.imageCount === 0 && bike.saleStatus !== 'SOLD' && (
          <div className="mt-2 text-xs text-amber-600 flex items-center gap-1.5 bg-amber-50 rounded-lg px-2 py-1.5">
            <AlertCircle size={12} /> Upload image to enable sale
          </div>
        )}

        {/* Action buttons */}
        <div className="mt-3 pt-3 border-t border-gray-100 space-y-2">
          {/* Sale status actions */}
          <div className="flex flex-wrap gap-1.5">
            {bike.saleStatus === 'NOT_FOR_SALE' && (
              <button
                onClick={() => bike.imageCount > 0 ? onSaleAction('enable', bike.id) : alert('Upload at least one image first.')}
                disabled={bike.imageCount === 0}
                className={`flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg font-medium transition-colors
                  ${bike.imageCount > 0
                    ? 'bg-emerald-100 text-emerald-700 hover:bg-emerald-200'
                    : 'bg-gray-100 text-gray-400 cursor-not-allowed'}`}
                title={bike.imageCount === 0 ? 'No Image â€” Upload Image First' : 'Enable Sale'}>
                <CheckCircle size={12} /> Enable Sale
              </button>
            )}
            {bike.saleStatus === 'AVAILABLE' && (
              <>
                <button onClick={() => onSaleAction('reserve', bike.id)}
                  className="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-amber-100 text-amber-700 hover:bg-amber-200 font-medium transition-colors">
                  <Tag size={12} /> Reserve
                </button>
                <button onClick={() => onSaleAction('sold', bike.id)}
                  className="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-red-100 text-red-600 hover:bg-red-200 font-medium transition-colors">
                  Sold
                </button>
                <button onClick={() => onSaleAction('disable', bike.id)}
                  className="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-gray-100 text-gray-600 hover:bg-gray-200 font-medium transition-colors">
                  Disable
                </button>
              </>
            )}
            {bike.saleStatus === 'RESERVED' && (
              <>
                <button onClick={() => onSaleAction('sold', bike.id)}
                  className="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-red-100 text-red-600 hover:bg-red-200 font-medium transition-colors">
                  Mark Sold
                </button>
                <button onClick={() => onSaleAction('enable', bike.id)}
                  className="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-emerald-100 text-emerald-700 hover:bg-emerald-200 font-medium transition-colors">
                  Re-list
                </button>
                <button onClick={() => onSaleAction('disable', bike.id)}
                  className="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-gray-100 text-gray-600 hover:bg-gray-200 font-medium transition-colors">
                  Disable
                </button>
              </>
            )}
          </div>
          {/* Utility buttons */}
          <div className="flex items-center gap-1.5">
            <button onClick={() => onImages(bike)}
              className="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-blue-50 text-blue-600 hover:bg-blue-100 font-medium transition-colors">
              <Image size={12} /> Images {bike.imageCount > 0 && `(${bike.imageCount})`}
            </button>
            <button onClick={() => onEdit(bike)}
              className="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-gray-100 text-gray-600 hover:bg-gray-200 font-medium transition-colors">
              <Edit2 size={12} /> Edit
            </button>
            {bike.saleStatus !== 'AVAILABLE' && bike.saleStatus !== 'RESERVED' && (
              <button onClick={() => onDelete(bike.id)}
                className="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-red-50 text-red-500 hover:bg-red-100 font-medium transition-colors ml-auto">
                <Trash2 size={12} />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
