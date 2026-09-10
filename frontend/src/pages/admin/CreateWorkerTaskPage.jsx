import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { adminCreateTask } from '../../api/workerTaskApi';
import api from '../../api/axios';

const CreateWorkerTaskPage = () => {
  const navigate = useNavigate();
  const [saving, setSaving] = useState(false);
  const [workers, setWorkers] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [bikes, setBikes] = useState([]);
  const [form, setForm] = useState({
    taskType: 'REPAIR', title: '', description: '', priority: 'NORMAL',
    workerId: '', customerId: '', bikeInventoryId: '',
    dueDate: '', adminNotes: '',
    amountDue: '', outstandingAmount: '', reason: '', authorizationNumber: ''
  });

  useEffect(() => {
    const load = async () => {
      try {
        const [wRes, cRes, bRes] = await Promise.all([
          api.get('/admin/workers?size=100'),
          api.get('/admin/customers?size=100'),
          api.get('/bikes?size=100'),
        ]);
        setWorkers(wRes.data?.data?.content || wRes.data?.data || []);
        setCustomers(cRes.data?.data?.content || cRes.data?.data || []);
        setBikes(bRes.data?.data?.content || bRes.data?.data || []);
      } catch { /* ignore */ }
    };
    load();
  }, []);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.workerId) { toast.error('Select a worker'); return; }
    if (!form.title.trim()) { toast.error('Enter a task title'); return; }
    setSaving(true);
    try {
      await adminCreateTask({
        taskType: form.taskType,
        title: form.title,
        description: form.description,
        priority: form.priority,
        workerId: parseInt(form.workerId),
        customerId: form.customerId ? parseInt(form.customerId) : null,
        bikeInventoryId: form.bikeInventoryId ? parseInt(form.bikeInventoryId) : null,
        dueDate: form.dueDate || null,
        adminNotes: form.adminNotes,
        amountDue: form.amountDue ? parseFloat(form.amountDue) : null,
        outstandingAmount: form.outstandingAmount ? parseFloat(form.outstandingAmount) : null,
        reason: form.reason || null,
        authorizationNumber: form.authorizationNumber || null,
      });
      toast.success('Task assigned!');
      navigate('/admin/worker-tasks');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create task');
    } finally { setSaving(false); }
  };

  const inp = "w-full px-3 py-2 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none";

  return (
    <div className="max-w-3xl space-y-6">
      <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-gray-500 hover:text-gray-800 text-sm">
        <ArrowLeft size={16}/> Back to Tasks
      </button>
      <div>
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <Plus className="text-[#1E88E5]" size={24}/> Assign Field Task
        </h1>
        <p className="text-gray-500 text-sm mt-1">Create a new task and assign it to a worker</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
          <h2 className="font-semibold text-gray-700">Task Details</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Task Type *</label>
              <select value={form.taskType} onChange={e => set('taskType', e.target.value)} className={inp}>
                <option value="REPAIR">Repair / Service</option>
                <option value="COLLECTION">Payment Collection</option>
                <option value="VISIT">Customer Visit (Overdue)</option>
                <option value="RECOVERY">Bike Recovery</option>
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Priority</label>
              <select value={form.priority} onChange={e => set('priority', e.target.value)} className={inp}>
                <option value="LOW">Low</option>
                <option value="NORMAL">Normal</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Title *</label>
              <input required value={form.title} onChange={e => set('title', e.target.value)}
                className={inp} placeholder="e.g. Repair engine oil leak"/>
            </div>
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Description</label>
              <textarea value={form.description} onChange={e => set('description', e.target.value)}
                rows={3} className={inp} placeholder="Detailed task description..."/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Due Date</label>
              <input type="date" value={form.dueDate} onChange={e => set('dueDate', e.target.value)} className={inp}/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Admin Notes</label>
              <input value={form.adminNotes} onChange={e => set('adminNotes', e.target.value)}
                className={inp} placeholder="Internal notes for worker"/>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
          <h2 className="font-semibold text-gray-700">Assignment</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Assign To Worker *</label>
              <select required value={form.workerId} onChange={e => set('workerId', e.target.value)} className={inp}>
                <option value="">-- Select Worker --</option>
                {(workers || []).map(w => (
                  <option key={w.id || w.userId} value={w.id || w.userId}>
                    {w.firstName || w.name || 'Worker'} {w.lastName || ''}
                    {w.employeeId ? ' (' + w.employeeId + ')' : ''}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Customer (optional)</label>
              <select value={form.customerId} onChange={e => set('customerId', e.target.value)} className={inp}>
                <option value="">-- Select Customer --</option>
                {(customers || []).map(c => (
                  <option key={c.id} value={c.id}>
                    {c.firstName || c.name} {c.lastName || ''} -- {c.email || c.phone}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Bike (optional)</label>
              <select value={form.bikeInventoryId} onChange={e => set('bikeInventoryId', e.target.value)} className={inp}>
                <option value="">-- Select Bike --</option>
                {(bikes || []).map(b => (
                  <option key={b.id} value={b.id}>
                    {b.bikeCode} -- {b.modelName || b.bikeModel} ({b.registrationNumber || 'No Reg'})
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {form.taskType === 'COLLECTION' && (
          <div className="bg-green-50 border border-green-200 rounded-2xl p-6 space-y-4">
            <h2 className="font-semibold text-green-800">Collection Details</h2>
            <div>
              <label className="text-xs text-green-700 font-medium block mb-1">Amount Due (Rs.)</label>
              <input type="number" value={form.amountDue} onChange={e => set('amountDue', e.target.value)} className={inp}/>
            </div>
          </div>
        )}

        {form.taskType === 'RECOVERY' && (
          <div className="bg-red-50 border border-red-200 rounded-2xl p-6 space-y-4">
            <h2 className="font-semibold text-red-800">Recovery Authorization</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-xs text-red-700 font-medium block mb-1">Outstanding Amount (Rs.)</label>
                <input type="number" value={form.outstandingAmount} onChange={e => set('outstandingAmount', e.target.value)} className={inp}/>
              </div>
              <div>
                <label className="text-xs text-red-700 font-medium block mb-1">Authorization / Order #</label>
                <input value={form.authorizationNumber} onChange={e => set('authorizationNumber', e.target.value)} className={inp}/>
              </div>
              <div className="md:col-span-2">
                <label className="text-xs text-red-700 font-medium block mb-1">Reason for Recovery *</label>
                <textarea value={form.reason} onChange={e => set('reason', e.target.value)}
                  rows={2} className={inp} placeholder="Legal/contractual reason for repossession..."/>
              </div>
            </div>
          </div>
        )}

        <div className="flex items-center gap-3">
          <button type="submit" disabled={saving}
            className="flex items-center gap-2 px-6 py-2.5 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-xl text-sm font-semibold disabled:opacity-50">
            <Plus size={16}/> {saving ? 'Assigning...' : 'Assign Task'}
          </button>
          <button type="button" onClick={() => navigate(-1)}
            className="px-6 py-2.5 border border-gray-200 text-gray-600 rounded-xl text-sm font-semibold hover:bg-gray-50">
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};
export default CreateWorkerTaskPage;