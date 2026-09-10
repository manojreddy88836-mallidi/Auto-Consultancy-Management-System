import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Trash2 } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { adminGetTaskDetail, adminCancelTask } from '../../api/workerTaskApi';

const Field = ({ label, value }) => (
  <div>
    <p className="text-xs text-gray-400 font-medium uppercase tracking-wide">{label}</p>
    <p className="text-sm text-gray-800 mt-0.5">{value != null && value !== '' ? String(value) : '--'}</p>
  </div>
);

const Section = ({ title, children, border = 'border-gray-100' }) => (
  <div className={`bg-white rounded-2xl border ${border} shadow-sm p-6 space-y-4`}>
    <h2 className="font-semibold text-gray-700">{title}</h2>
    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">{children}</div>
  </div>
);

const WorkerTaskDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const res = await adminGetTaskDetail(id);
      setDetail(res.data?.data);
    } catch { toast.error('Failed to load task'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [id]);

  const cancel = async () => {
    if (!window.confirm('Cancel this task?')) return;
    try {
      await adminCancelTask(id);
      toast.success('Task cancelled');
      navigate('/admin/worker-tasks');
    } catch { toast.error('Failed to cancel task'); }
  };

  if (loading) return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/></div>;
  if (!detail) return <div className="text-center py-20 text-gray-400">Task not found</div>;

  const t = detail.task;
  const fmt = (v) => v ? new Date(v).toLocaleDateString('en-IN') : null;
  const cur = (v) => v != null ? `Rs.${parseFloat(v).toLocaleString('en-IN')}` : null;

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center justify-between">
        <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-gray-500 hover:text-gray-800 text-sm"><ArrowLeft size={16}/> Back</button>
        {t?.status !== 'CANCELLED' && (
          <button onClick={cancel} className="flex items-center gap-2 px-4 py-2 bg-red-50 hover:bg-red-100 text-red-600 rounded-xl text-sm font-semibold border border-red-200">
            <Trash2 size={14}/> Cancel Task
          </button>
        )}
      </div>

      <Section title="Task Overview">
        <Field label="Title" value={t?.title} />
        <Field label="Type" value={t?.taskType} />
        <Field label="Status" value={t?.status?.replace(/_/g,' ')} />
        <Field label="Priority" value={t?.priority} />
        <Field label="Due Date" value={fmt(t?.dueDate)} />
        <Field label="Completed At" value={t?.completedAt ? new Date(t.completedAt).toLocaleString('en-IN') : null} />
        <div className="col-span-2 md:col-span-3">
          <p className="text-xs text-gray-400 font-medium uppercase tracking-wide">Description</p>
          <p className="text-sm text-gray-700 mt-0.5">{t?.description || '--'}</p>
        </div>
        <div className="col-span-2 md:col-span-3">
          <p className="text-xs text-gray-400 font-medium uppercase tracking-wide">Worker Notes</p>
          <p className="text-sm text-gray-700 mt-0.5">{t?.workerNotes || '--'}</p>
        </div>
      </Section>

      <Section title="Assigned Worker & Customer">
        <Field label="Worker" value={t?.workerName} />
        <Field label="Customer" value={t?.customerName} />
        <Field label="Customer Phone" value={t?.customerPhone} />
        <Field label="Bike" value={t?.bikeModel || t?.bikeCode} />
        <Field label="Registration" value={t?.registrationNumber} />
        <Field label="Application #" value={t?.applicationNumber} />
      </Section>

      {/* Service job details */}
      {detail.sjTaskDescription && (
        <Section title="Repair / Service Details" border="border-orange-100">
          <Field label="Description" value={detail.sjTaskDescription} />
          <Field label="Parts Used" value={detail.sjPartsUsed} />
          <Field label="Labour" value={cur(detail.sjLabourCharge)} />
          <Field label="Parts Cost" value={cur(detail.sjPartsCharge)} />
          <Field label="Total" value={cur(detail.sjTotalAmount)} />
          <Field label="Payment Status" value={detail.sjPaymentStatus} />
          <Field label="Service Date" value={fmt(detail.sjServiceDate)} />
          <Field label="Completion Date" value={fmt(detail.sjCompletionDate)} />
          <Field label="Notes" value={detail.sjNotes} />
        </Section>
      )}

      {/* Payment collection */}
      {detail.pcAmountDue != null && (
        <Section title="Payment Collection Details" border="border-green-100">
          <Field label="Amount Due" value={cur(detail.pcAmountDue)} />
          <Field label="Amount Collected" value={cur(detail.pcAmountCollected)} />
          <Field label="Payment Method" value={detail.pcPaymentMethod} />
          <Field label="Collection Date" value={fmt(detail.pcCollectionDate)} />
          <Field label="Receipt #" value={detail.pcReceiptNumber} />
          <Field label="UPI Reference" value={detail.pcUpiReference} />
          <Field label="Notes" value={detail.pcNotes} />
          <Field label="Verified by Admin" value={detail.pcVerifiedByAdmin ? 'Yes' : 'No'} />
        </Section>
      )}

      {/* Field visit */}
      {detail.fvVisitDate && (
        <Section title="Field Visit Details" border="border-blue-100">
          <Field label="Visit Date" value={fmt(detail.fvVisitDate)} />
          <Field label="Customer Contacted" value={detail.fvCustomerContacted ? 'Yes' : 'No'} />
          <Field label="Amount Collected" value={cur(detail.fvAmountCollected)} />
          <Field label="Promise-to-Pay Date" value={fmt(detail.fvPromiseToPayDate)} />
          <Field label="Promise Amount" value={cur(detail.fvPromiseAmount)} />
          <Field label="Customer Response" value={detail.fvCustomerResponse} />
          <Field label="Outcome Notes" value={detail.fvOutcomeNotes} />
        </Section>
      )}

      {/* Bike recovery */}
      {detail.brReason && (
        <Section title="Bike Recovery Details" border="border-red-100">
          <Field label="Authorization #" value={detail.brAuthorizationNumber} />
          <Field label="Outstanding Amount" value={cur(detail.brOutstandingAmount)} />
          <Field label="Reason" value={detail.brReason} />
          <Field label="Recovery Date" value={fmt(detail.brRecoveryDate)} />
          <Field label="Bike Condition" value={detail.brBikeCondition} />
          <Field label="Mileage" value={detail.brCurrentMileage ? `${detail.brCurrentMileage} km` : null} />
          <Field label="Existing Damage" value={detail.brExistingDamage} />
          <Field label="Accessories Received" value={detail.brAccessoriesReceived ? 'Yes' : 'No'} />
          <Field label="Keys Received" value={detail.brKeysReceived ? 'Yes' : 'No'} />
          <Field label="Docs Received" value={detail.brDocumentsReceived ? 'Yes' : 'No'} />
          <Field label="Customer Acknowledged" value={detail.brCustomerAcknowledged ? 'Yes' : 'No'} />
          <Field label="Worker Notes" value={detail.brWorkerRecoveryNotes} />
        </Section>
      )}
    </div>
  );
};
export default WorkerTaskDetailPage;