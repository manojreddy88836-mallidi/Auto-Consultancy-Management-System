import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, RefreshCw, Wrench, CreditCard, Users, ShieldAlert } from 'lucide-react';
import { toast } from 'react-hot-toast';
import {
  getTaskDetail, updateTaskStatus,
  updateServiceJob, recordPayment, updateFieldVisit, updateBikeRecovery
} from '../../api/workerTaskApi';

const Field = ({ label, value }) => (
  <div>
    <p className="text-xs text-gray-400 font-medium uppercase tracking-wide">{label}</p>
    <p className="text-sm text-gray-800 mt-0.5">{value || '--'}</p>
  </div>
);

const Section = ({ title, children }) => (
  <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
    <h2 className="font-semibold text-gray-700 text-base">{title}</h2>
    {children}
  </div>
);

const TASK_STATUSES = {
  REPAIR:     ['ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED'],
  COLLECTION: ['ASSIGNED','IN_PROGRESS','PAYMENT_COLLECTED','CUSTOMER_UNAVAILABLE','CANCELLED'],
  VISIT:      ['ASSIGNED','VISITED','PAYMENT_COLLECTED','PROMISED_TO_PAY','CUSTOMER_UNAVAILABLE','ESCALATED','COMPLETED'],
  RECOVERY:   ['RECOVERY_ASSIGNED','CUSTOMER_CONTACTED','RECOVERY_SCHEDULED','BIKE_RECOVERED','CUSTOMER_PAID','CUSTOMER_UNAVAILABLE','ESCALATED','CANCELLED'],
};

const TaskDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Form states
  const [status, setStatus] = useState('');
  const [workerNotes, setWorkerNotes] = useState('');

  // Service job
  const [sj, setSj] = useState({ taskDescription:'', partsUsed:'', labourCharge:'', partsCharge:'', totalAmount:'', paymentStatus:'PENDING', serviceDate:'', completionDate:'', notes:'' });

  // Payment
  const [pc, setPc] = useState({ amountCollected:'', paymentMethod:'CASH', collectionDate:'', receiptNumber:'', upiReference:'', notes:'' });

  // Field visit
  const [fv, setFv] = useState({ visitDate:'', customerContacted:false, amountCollected:'', promiseToPayDate:'', promiseAmount:'', customerResponse:'', outcomeNotes:'', newStatus:'' });

  // Recovery
  const [br, setBr] = useState({ recoveryDate:'', bikeCondition:'', currentMileage:'', existingDamage:'', accessoriesReceived:false, keysReceived:false, documentsReceived:false, workerRecoveryNotes:'', customerAcknowledged:false, newStatus:'' });

  const load = async () => {
    setLoading(true);
    try {
      const res = await getTaskDetail(id);
      const d = res.data?.data;
      setDetail(d);
      setStatus(d?.task?.status || '');
      setWorkerNotes(d?.task?.workerNotes || '');
      if (d?.sjTaskDescription) setSj(prev => ({ ...prev, taskDescription: d.sjTaskDescription||'', partsUsed:d.sjPartsUsed||'', labourCharge:d.sjLabourCharge||'', partsCharge:d.sjPartsCharge||'', totalAmount:d.sjTotalAmount||'', paymentStatus:d.sjPaymentStatus||'PENDING', serviceDate:d.sjServiceDate||'', completionDate:d.sjCompletionDate||'', notes:d.sjNotes||'' }));
      if (d?.pcAmountDue)     setPc(prev => ({ ...prev, amountCollected:d.pcAmountCollected||'', paymentMethod:d.pcPaymentMethod||'CASH', collectionDate:d.pcCollectionDate||'', receiptNumber:d.pcReceiptNumber||'', upiReference:d.pcUpiReference||'', notes:d.pcNotes||'' }));
      if (d?.fvVisitDate)    setFv(prev => ({ ...prev, visitDate:d.fvVisitDate||'', customerContacted:d.fvCustomerContacted||false, amountCollected:d.fvAmountCollected||'', promiseToPayDate:d.fvPromiseToPayDate||'', promiseAmount:d.fvPromiseAmount||'', customerResponse:d.fvCustomerResponse||'', outcomeNotes:d.fvOutcomeNotes||'' }));
      if (d?.brReason)       setBr(prev => ({ ...prev, bikeCondition:d.brBikeCondition||'', currentMileage:d.brCurrentMileage||'', existingDamage:d.brExistingDamage||'', accessoriesReceived:d.brAccessoriesReceived||false, keysReceived:d.brKeysReceived||false, documentsReceived:d.brDocumentsReceived||false, workerRecoveryNotes:d.brWorkerRecoveryNotes||'', customerAcknowledged:d.brCustomerAcknowledged||false }));
    } catch { toast.error('Failed to load task'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [id]);

  const saveStatus = async () => {
    setSaving(true);
    try {
      await updateTaskStatus(id, { status, workerNotes });
      toast.success('Status updated!');
      load();
    } catch { toast.error('Failed to update status'); }
    finally { setSaving(false); }
  };

  const saveService = async () => {
    setSaving(true);
    try {
      await updateServiceJob(id, sj);
      toast.success('Service job saved!');
      load();
    } catch { toast.error('Failed to save service job'); }
    finally { setSaving(false); }
  };

  const savePayment = async () => {
    setSaving(true);
    try {
      await recordPayment(id, pc);
      toast.success('Payment recorded!');
      load();
    } catch { toast.error('Failed to record payment'); }
    finally { setSaving(false); }
  };

  const saveVisit = async () => {
    setSaving(true);
    try {
      await updateFieldVisit(id, fv);
      toast.success('Visit updated!');
      load();
    } catch { toast.error('Failed to update visit'); }
    finally { setSaving(false); }
  };

  const saveRecovery = async () => {
    setSaving(true);
    try {
      await updateBikeRecovery(id, br);
      toast.success('Recovery updated!');
      load();
    } catch { toast.error('Failed to update recovery'); }
    finally { setSaving(false); }
  };

  if (loading) return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/></div>;
  if (!detail) return <div className="text-center py-20 text-gray-400">Task not found</div>;

  const task = detail.task;
  const type = task?.taskType;
  const Icon = { REPAIR:Wrench, COLLECTION:CreditCard, VISIT:Users, RECOVERY:ShieldAlert }[type] || RefreshCw;
  const availableStatuses = TASK_STATUSES[type] || ['ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED'];

  const inputCls = "w-full px-3 py-2 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none";
  const btnCls = "flex items-center gap-2 px-4 py-2 bg-[#1E88E5] hover:bg-[#1976D2] text-white rounded-xl text-sm font-semibold disabled:opacity-50";

  return (
    <div className="space-y-6 max-w-4xl">
      <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-gray-500 hover:text-gray-800 text-sm">
        <ArrowLeft size={16}/> Back to Tasks
      </button>

      {/* Header */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-[#1E88E5]/10 flex items-center justify-center flex-shrink-0">
            <Icon size={22} className="text-[#1E88E5]"/>
          </div>
          <div className="flex-1 min-w-0">
            <h1 className="text-xl font-bold text-gray-900">{task.title}</h1>
            <p className="text-sm text-gray-500 mt-1">{task.description}</p>
            <div className="flex flex-wrap gap-3 mt-3 text-xs">
              <span className="px-2 py-1 bg-gray-100 rounded-full font-medium">{type}</span>
              <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded-full font-medium">{task.status?.replace(/_/g,' ')}</span>
              <span className="px-2 py-1 bg-orange-50 text-orange-600 rounded-full font-medium">Priority: {task.priority}</span>
              {task.dueDate && <span className="px-2 py-1 bg-red-50 text-red-600 rounded-full font-medium">Due: {new Date(task.dueDate).toLocaleDateString('en-IN')}</span>}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mt-5 pt-5 border-t border-gray-100">
          <Field label="Customer" value={task.customerName} />
          <Field label="Phone" value={task.customerPhone} />
          <Field label="Bike" value={task.bikeModel || task.bikeCode} />
          <Field label="Registration" value={task.registrationNumber} />
          <Field label="Application" value={task.applicationNumber} />
          <Field label="Admin Notes" value={task.adminNotes} />
        </div>
      </div>

      {/* Status Update */}
      <Section title="Update Status & Notes">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="text-xs text-gray-500 font-medium block mb-1">Status</label>
            <select value={status} onChange={e => setStatus(e.target.value)} className={inputCls}>
              {availableStatuses.map(s => <option key={s} value={s}>{s.replace(/_/g,' ')}</option>)}
            </select>
          </div>
          <div>
            <label className="text-xs text-gray-500 font-medium block mb-1">Your Notes</label>
            <textarea value={workerNotes} onChange={e => setWorkerNotes(e.target.value)} rows={2} className={inputCls} placeholder="Add field notes..."/>
          </div>
        </div>
        <button onClick={saveStatus} disabled={saving} className={btnCls}>
          <Save size={15}/> Save Status
        </button>
      </Section>

      {/* REPAIR specific */}
      {type === 'REPAIR' && (
        <Section title="Repair / Service Details">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Task Description</label>
              <textarea value={sj.taskDescription} onChange={e => setSj({...sj,taskDescription:e.target.value})} rows={3} className={inputCls} placeholder="Describe the repair/service work done..."/>
            </div>
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Parts / Materials Used</label>
              <textarea value={sj.partsUsed} onChange={e => setSj({...sj,partsUsed:e.target.value})} rows={2} className={inputCls} placeholder="List parts used..."/>
            </div>
            {[['Labour Charge (Rs.)','labourCharge'],['Parts Charge (Rs.)','partsCharge'],['Total Amount (Rs.)','totalAmount']].map(([lbl,key]) => (
              <div key={key}>
                <label className="text-xs text-gray-500 font-medium block mb-1">{lbl}</label>
                <input type="number" value={sj[key]} onChange={e => setSj({...sj,[key]:e.target.value})} className={inputCls}/>
              </div>
            ))}
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Payment Status</label>
              <select value={sj.paymentStatus} onChange={e => setSj({...sj,paymentStatus:e.target.value})} className={inputCls}>
                <option value="PENDING">Pending</option>
                <option value="PARTIAL">Partial</option>
                <option value="PAID">Paid</option>
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Service Date</label>
              <input type="date" value={sj.serviceDate} onChange={e => setSj({...sj,serviceDate:e.target.value})} className={inputCls}/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Completion Date</label>
              <input type="date" value={sj.completionDate} onChange={e => setSj({...sj,completionDate:e.target.value})} className={inputCls}/>
            </div>
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Notes</label>
              <textarea value={sj.notes} onChange={e => setSj({...sj,notes:e.target.value})} rows={2} className={inputCls}/>
            </div>
          </div>
          <button onClick={saveService} disabled={saving} className={btnCls}><Save size={15}/> Save Service Details</button>
        </Section>
      )}

      {/* COLLECTION specific */}
      {type === 'COLLECTION' && (
        <Section title="Record Payment Collection">
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm">
            <p className="font-semibold text-amber-800">Amount Due: Rs.{detail.pcAmountDue?.toLocaleString('en-IN') || task.amountDue?.toLocaleString('en-IN') || '--'}</p>
            <p className="text-amber-600 text-xs mt-0.5">Collecting payment on behalf of the company. Outstanding balance will be updated automatically.</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Amount Collected (Rs.) *</label>
              <input type="number" value={pc.amountCollected} onChange={e => setPc({...pc,amountCollected:e.target.value})} className={inputCls} placeholder="0.00"/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Payment Method</label>
              <select value={pc.paymentMethod} onChange={e => setPc({...pc,paymentMethod:e.target.value})} className={inputCls}>
                <option value="CASH">Cash</option>
                <option value="UPI">UPI</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Collection Date</label>
              <input type="date" value={pc.collectionDate} onChange={e => setPc({...pc,collectionDate:e.target.value})} className={inputCls}/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Receipt / Reference No.</label>
              <input value={pc.receiptNumber} onChange={e => setPc({...pc,receiptNumber:e.target.value})} className={inputCls}/>
            </div>
            {pc.paymentMethod === 'UPI' && (
              <div>
                <label className="text-xs text-gray-500 font-medium block mb-1">UPI Reference</label>
                <input value={pc.upiReference} onChange={e => setPc({...pc,upiReference:e.target.value})} className={inputCls}/>
              </div>
            )}
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Notes</label>
              <textarea value={pc.notes} onChange={e => setPc({...pc,notes:e.target.value})} rows={2} className={inputCls}/>
            </div>
          </div>
          <button onClick={savePayment} disabled={saving || !pc.amountCollected} className={btnCls}><Save size={15}/> Record Payment</button>
        </Section>
      )}

      {/* VISIT specific */}
      {type === 'VISIT' && (
        <Section title="Field Visit Details">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Visit Date</label>
              <input type="date" value={fv.visitDate} onChange={e => setFv({...fv,visitDate:e.target.value})} className={inputCls}/>
            </div>
            <div className="flex items-center gap-3 pt-5">
              <input type="checkbox" id="contacted" checked={fv.customerContacted} onChange={e => setFv({...fv,customerContacted:e.target.checked})} className="w-4 h-4 accent-[#1E88E5]"/>
              <label htmlFor="contacted" className="text-sm text-gray-700 font-medium">Customer was contacted / present</label>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Amount Collected (Rs.)</label>
              <input type="number" value={fv.amountCollected} onChange={e => setFv({...fv,amountCollected:e.target.value})} className={inputCls}/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Promise-to-Pay Date</label>
              <input type="date" value={fv.promiseToPayDate} onChange={e => setFv({...fv,promiseToPayDate:e.target.value})} className={inputCls}/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Promise Amount (Rs.)</label>
              <input type="number" value={fv.promiseAmount} onChange={e => setFv({...fv,promiseAmount:e.target.value})} className={inputCls}/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Update Status To</label>
              <select value={fv.newStatus} onChange={e => setFv({...fv,newStatus:e.target.value})} className={inputCls}>
                <option value="">-- no change --</option>
                {TASK_STATUSES.VISIT.map(s => <option key={s} value={s}>{s.replace(/_/g,' ')}</option>)}
              </select>
            </div>
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Customer Response</label>
              <textarea value={fv.customerResponse} onChange={e => setFv({...fv,customerResponse:e.target.value})} rows={2} className={inputCls}/>
            </div>
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Outcome Notes</label>
              <textarea value={fv.outcomeNotes} onChange={e => setFv({...fv,outcomeNotes:e.target.value})} rows={2} className={inputCls}/>
            </div>
          </div>
          <button onClick={saveVisit} disabled={saving} className={btnCls}><Save size={15}/> Save Visit</button>
        </Section>
      )}

      {/* RECOVERY specific */}
      {type === 'RECOVERY' && (
        <Section title="Bike Recovery Details">
          <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm mb-4">
            <p className="font-semibold text-red-800">Authorization: {detail.brAuthorizationNumber || task.adminNotes || '--'}</p>
            <p className="text-red-600 text-xs mt-0.5">Outstanding Amount: Rs.{detail.brOutstandingAmount?.toLocaleString('en-IN') || '--'}</p>
            <p className="text-red-600 text-xs">Reason: {detail.brReason || '--'}</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Recovery Date</label>
              <input type="date" value={br.recoveryDate} onChange={e => setBr({...br,recoveryDate:e.target.value})} className={inputCls}/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Bike Condition</label>
              <select value={br.bikeCondition} onChange={e => setBr({...br,bikeCondition:e.target.value})} className={inputCls}>
                <option value="">Select...</option>
                {['EXCELLENT','GOOD','FAIR','POOR','DAMAGED'].map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Current Mileage (km)</label>
              <input type="number" value={br.currentMileage} onChange={e => setBr({...br,currentMileage:e.target.value})} className={inputCls}/>
            </div>
            <div>
              <label className="text-xs text-gray-500 font-medium block mb-1">Update Status</label>
              <select value={br.newStatus} onChange={e => setBr({...br,newStatus:e.target.value})} className={inputCls}>
                <option value="">-- no change --</option>
                {TASK_STATUSES.RECOVERY.map(s => <option key={s} value={s}>{s.replace(/_/g,' ')}</option>)}
              </select>
            </div>
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Existing Damage</label>
              <textarea value={br.existingDamage} onChange={e => setBr({...br,existingDamage:e.target.value})} rows={2} className={inputCls}/>
            </div>
            <div className="flex flex-wrap gap-6">
              {[['accessoriesReceived','Accessories Received'],['keysReceived','Keys Received'],['documentsReceived','Docs Received'],['customerAcknowledged','Customer Acknowledged']].map(([k,lbl]) => (
                <label key={k} className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input type="checkbox" checked={br[k]} onChange={e => setBr({...br,[k]:e.target.checked})} className="w-4 h-4 accent-[#1E88E5]"/>
                  {lbl}
                </label>
              ))}
            </div>
            <div className="md:col-span-2">
              <label className="text-xs text-gray-500 font-medium block mb-1">Recovery Notes</label>
              <textarea value={br.workerRecoveryNotes} onChange={e => setBr({...br,workerRecoveryNotes:e.target.value})} rows={3} className={inputCls}/>
            </div>
          </div>
          <button onClick={saveRecovery} disabled={saving} className={btnCls}><Save size={15}/> Save Recovery</button>
        </Section>
      )}
    </div>
  );
};

export default TaskDetailPage;