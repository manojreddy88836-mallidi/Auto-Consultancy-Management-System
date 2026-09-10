import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Pencil, ToggleLeft, ToggleRight, UserCheck, Search, RefreshCw, Phone, Mail, Building } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getWorkers, createWorker, updateWorker, deactivateWorker } from '../../api/adminApi';

const DEPARTMENTS = ['Verification','Finance','Documentation','Operations','Support','Management'];
const Modal = ({ isOpen, onClose, title, children }) => !isOpen ? null : (
  <div className="fixed inset-0 z-50 flex items-center justify-center">
    <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose}/>
    <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 p-6 z-10">
      <h3 className="text-lg font-bold text-gray-900 mb-5">{title}</h3>
      {children}
    </div>
  </div>
);

const WorkersPage = () => {
  const [workers, setWorkers]       = useState([]);
  const [loading, setLoading]       = useState(true);
  const [search, setSearch]         = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingWorker, setEditingWorker] = useState(null);
  const [saving, setSaving]         = useState(false);
  const [form, setForm]             = useState({
    firstName:'', lastName:'', email:'', phone:'',
    employeeId:'', department:'', designation:'', password:'Worker@123',
  });

  const fetchWorkers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getWorkers();
      const d = res.data?.data;
      setWorkers(Array.isArray(d) ? d : d?.content || []);
    } catch { toast.error('Failed to load workers'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchWorkers(); }, [fetchWorkers]);

  const openAdd = () => {
    setEditingWorker(null);
    setForm({ firstName:'', lastName:'', email:'', phone:'', employeeId:'', department:'', designation:'', password:'Worker@123' });
    setIsModalOpen(true);
  };

  const openEdit = (w) => {
    setEditingWorker(w);
    setForm({
      firstName: w.user?.firstName || '',
      lastName:  w.user?.lastName  || '',
      email:     w.user?.email     || '',
      phone:     w.user?.phone     || '',
      employeeId:  w.employeeId    || '',
      department:  w.department    || '',
      designation: w.designation   || '',
      password: '',
    });
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!form.firstName || !form.email) { toast.error('First name and email are required'); return; }
    setSaving(true);
    try {
      if (editingWorker) {
        await updateWorker(editingWorker.id, form);
        toast.success('Worker updated');
      } else {
        await createWorker({ ...form, role: 'WORKER' });
        toast.success('Worker created — they can login with their email and the default password.');
      }
      setIsModalOpen(false);
      fetchWorkers();
    } catch (e) { toast.error(e?.response?.data?.message || 'Operation failed'); }
    finally { setSaving(false); }
  };

  const handleToggle = async (w) => {
    try {
      await deactivateWorker(w.id);
      toast.success(`${w.user?.firstName} ${w.active ? 'deactivated' : 'reactivated'}`);
      fetchWorkers();
    } catch { toast.error('Failed to update worker'); }
  };

  const filtered = workers.filter(w => {
    const q = search.toLowerCase();
    return !q || (w.user?.firstName + ' ' + w.user?.lastName).toLowerCase().includes(q)
      || w.user?.email?.toLowerCase().includes(q)
      || w.department?.toLowerCase().includes(q)
      || w.employeeId?.toLowerCase().includes(q);
  });

  const field = (label, key, type='text', req=false, placeholder='') => (
    <div>
      <label className="block text-sm font-semibold text-gray-700 mb-1.5">{label}{req && <span className="text-red-500 ml-0.5">*</span>}</label>
      <input required={req} type={type} value={form[key]} onChange={e => setForm({...form,[key]:e.target.value})}
        placeholder={placeholder}
        className="w-full px-4 py-2.5 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none text-sm"/>
    </div>
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <UserCheck className="text-[#1E88E5]" size={24}/> Workers
          </h1>
          <p className="text-gray-500 text-sm mt-1">{workers.length} employees registered</p>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={fetchWorkers} className="p-2 hover:bg-gray-100 rounded-lg transition-colors" title="Refresh">
            <RefreshCw size={18} className="text-gray-500"/>
          </button>
          <button onClick={openAdd}
            className="flex items-center gap-2 px-4 py-2.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-xl font-semibold text-sm transition-all shadow-md hover:shadow-lg">
            <Plus size={18}/> Add Worker
          </button>
        </div>
      </div>

      <div className="relative max-w-sm">
        <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search by name, email, dept..."
          className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1,2,3].map(i => <div key={i} className="bg-white rounded-2xl h-48 animate-pulse"/>)}
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400 bg-white rounded-2xl border border-gray-100">
          <UserCheck size={40} className="mb-3 opacity-25"/>
          <p className="text-sm font-medium">{search ? 'No workers match your search' : 'No workers yet'}</p>
          {!search && <button onClick={openAdd} className="mt-3 text-[#1E88E5] text-sm hover:underline">Add first worker</button>}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map(w => (
            <div key={w.id} className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-3">
                  <div className="w-11 h-11 rounded-full bg-gradient-to-br from-[#0F1B35] to-[#1E88E5] flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
                    {w.user?.firstName?.[0]}{w.user?.lastName?.[0]}
                  </div>
                  <div>
                    <p className="font-bold text-gray-900">{w.user?.firstName} {w.user?.lastName}</p>
                    <p className="text-xs text-gray-400 font-mono">{w.employeeId || 'EMP—'}</p>
                  </div>
                </div>
                <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${w.active !== false ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-500'}`}>
                  {w.active !== false ? 'Active' : 'Inactive'}
                </span>
              </div>
              <div className="space-y-2 text-sm text-gray-600">
                <div className="flex items-center gap-2"><Mail size={13} className="text-gray-400 flex-shrink-0"/><span className="truncate">{w.user?.email}</span></div>
                <div className="flex items-center gap-2"><Phone size={13} className="text-gray-400 flex-shrink-0"/><span>{w.user?.phone || '—'}</span></div>
                <div className="flex items-center gap-2"><Building size={13} className="text-gray-400 flex-shrink-0"/><span>{w.department || '—'} · {w.designation || '—'}</span></div>
              </div>
              <div className="flex items-center gap-2 mt-4 pt-4 border-t border-gray-100">
                <button onClick={() => openEdit(w)}
                  className="flex-1 flex items-center justify-center gap-1.5 py-1.5 text-xs font-semibold text-blue-600 hover:bg-blue-50 rounded-lg transition-colors">
                  <Pencil size={12}/> Edit
                </button>
                <button onClick={() => handleToggle(w)}
                  className={`flex-1 flex items-center justify-center gap-1.5 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                    w.active !== false ? 'text-red-500 hover:bg-red-50' : 'text-green-600 hover:bg-green-50'
                  }`}>
                  {w.active !== false ? <><ToggleLeft size={14}/> Deactivate</> : <><ToggleRight size={14}/> Activate</>}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingWorker ? 'Edit Worker' : 'Add New Worker'}>
        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            {field('First Name','firstName','text',true,'Rahul')}
            {field('Last Name','lastName','text',false,'Sharma')}
          </div>
          {field('Email Address','email','email',true,'rahul@autoconsultancy.com')}
          {field('Phone Number','phone','tel',false,'9876543210')}
          <div className="grid grid-cols-2 gap-4">
            {field('Employee ID','employeeId','text',false,'EMP001')}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Department</label>
              <select value={form.department} onChange={e => setForm({...form,department:e.target.value})}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
                <option value="">Select</option>
                {DEPARTMENTS.map(d => <option key={d} value={d}>{d}</option>)}
              </select>
            </div>
          </div>
          {field('Designation','designation','text',false,'Senior Verifier')}
          {!editingWorker && field('Password','password','password',true,'Min 8 chars')}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setIsModalOpen(false)}
              className="px-5 py-2.5 rounded-xl border border-gray-200 text-sm font-medium hover:bg-gray-50 transition-colors">Cancel</button>
            <button type="submit" disabled={saving}
              className="px-5 py-2.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white text-sm font-semibold transition-all disabled:opacity-60 shadow-md">
              {saving ? 'Saving...' : editingWorker ? 'Update Worker' : 'Create Worker'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default WorkersPage;
