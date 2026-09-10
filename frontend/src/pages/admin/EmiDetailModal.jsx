import React, { useState, useEffect, useMemo } from 'react';
import { X, CheckCircle2, XCircle, Clock, Trash2, AlertTriangle, IndianRupee } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getEmiPayments, deleteEmiPayment } from '../../api/emiPaymentApi';
import { fmtINR } from '../../utils/emiCalculator';

// Overdue status helpers
const overdueConfig = {
  ON_TIME:    { label: 'On Time',               color: 'bg-green-100 text-green-700',  icon: '🟢', banner: null },
  ONE_MONTH:  { label: '1 Month Overdue',        color: 'bg-yellow-100 text-yellow-700', icon: '🟡', banner: 'yellow' },
  TWO_MONTHS: { label: '2 Months Overdue',       color: 'bg-orange-100 text-orange-700', icon: '🟠', banner: 'orange' },
  CRITICAL:   { label: 'Critical - Overdue',     color: 'bg-red-100 text-red-700',      icon: '🔴', banner: 'red' },
};

const fmt = (d) => d ? new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—';

/**
 * Full payment history & EMI schedule view for a financed application.
 *
 * Props:
 *  record   – ApplicationResponse object
 *  onClose  – fn()
 *  onRefresh – fn() — refresh parent after delete
 */
export default function EmiDetailModal({ record, onClose, onRefresh }) {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);

  const fd = record;
  const status = fd.overdueStatus || 'ON_TIME';
  const cfg = overdueConfig[status] || overdueConfig.ON_TIME;
  const missed = fd.missedEmiMonths || 0;

  useEffect(() => {
    if (!fd.financeDetailId) { setLoading(false); return; }
    getEmiPayments(fd.financeDetailId)
      .then(r => setPayments(r.data?.data || []))
      .catch(() => toast.error('Failed to load payment history'))
      .finally(() => setLoading(false));
  }, [fd.financeDetailId]);

  // Build set of paid installment numbers
  const paidMap = useMemo(() => {
    const m = {};
    payments.forEach(p => { m[p.installmentNumber] = p; });
    return m;
  }, [payments]);

  // Build EMI schedule: due dates from firstDueDate
  const schedule = useMemo(() => {
    const total = fd.numberOfEmis || 0;
    if (total === 0) return [];
    // Resolve first due date
    let firstDue = null;
    if (fd.loanStartDate) {
      const d = new Date(fd.loanStartDate);
      d.setMonth(d.getMonth() + 1);
      firstDue = d;
    } else if (fd.nextEmiDueDate) {
      const paid = payments.length;
      const d = new Date(fd.nextEmiDueDate);
      d.setMonth(d.getMonth() - paid);
      firstDue = d;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const items = [];
    for (let i = 1; i <= total; i++) {
      let dueDate = null;
      if (firstDue) {
        const d = new Date(firstDue);
        d.setMonth(d.getMonth() + (i - 1));
        dueDate = d;
      }
      const isPaid = !!paidMap[i];
      const isOverdue = dueDate && !isPaid && dueDate <= today;
      items.push({ installment: i, dueDate, isPaid, isOverdue, payment: paidMap[i] });
    }
    return items;
  }, [fd, payments, paidMap]);

  const handleDelete = async (paymentId, installNum) => {
    if (!window.confirm(`Delete payment for installment #${installNum}? This will recompute the overdue status.`)) return;
    try {
      await deleteEmiPayment(paymentId);
      toast.success('Payment deleted');
      setPayments(prev => prev.filter(p => p.id !== paymentId));
      onRefresh?.();
    } catch {
      toast.error('Failed to delete payment');
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col">

        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-gray-100 flex-shrink-0">
          <div>
            <h2 className="font-bold text-gray-900">EMI Payment History</h2>
            <p className="text-xs text-gray-500 mt-0.5">{fd.applicationNumber} · {fd.customerName}</p>
          </div>
          <button onClick={onClose} className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors">
            <X size={16} />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 p-5 space-y-5">

          {/* Overdue banner */}
          {status !== 'ON_TIME' && (
            <div className={`rounded-xl p-4 flex items-start gap-3 ${
              status === 'CRITICAL'   ? 'bg-red-50 border border-red-200' :
              status === 'TWO_MONTHS' ? 'bg-orange-50 border border-orange-200' :
              'bg-yellow-50 border border-yellow-200'
            }`}>
              <AlertTriangle size={18} className={
                status === 'CRITICAL'   ? 'text-red-600 flex-shrink-0 mt-0.5' :
                status === 'TWO_MONTHS' ? 'text-orange-600 flex-shrink-0 mt-0.5' :
                'text-yellow-600 flex-shrink-0 mt-0.5'
              } />
              <div>
                <p className={`font-bold text-sm ${
                  status === 'CRITICAL' ? 'text-red-700' :
                  status === 'TWO_MONTHS' ? 'text-orange-700' : 'text-yellow-700'
                }`}>
                  {cfg.icon} {cfg.label}
                </p>
                <p className="text-xs text-gray-600 mt-0.5">
                  {missed} EMI {missed === 1 ? 'installment has' : 'installments have'} not been paid.
                  Outstanding: {fmtINR(fd.outstandingLoanAmount)}
                </p>
              </div>
            </div>
          )}

          {/* Loan summary grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            {[
              { label: 'Loan Amount',    value: fmtINR(fd.loanAmount) },
              { label: 'Monthly EMI',    value: fmtINR(fd.emiAmount) },
              { label: 'Annual Rate',    value: fd.annualInterestRate != null ? `${fd.annualInterestRate}% p.a.` : '—' },
              { label: 'Tenure',         value: fd.tenureMonths ? `${fd.tenureMonths} months` : '—' },
              { label: 'Paid / Total',   value: `${payments.length} / ${fd.numberOfEmis || 0}` },
              { label: 'Outstanding',    value: fmtINR(fd.outstandingLoanAmount) },
              { label: 'Last Paid',      value: fmt(fd.lastEmiPaidDate) },
              { label: 'Next Due',       value: fmt(fd.nextEmiDueDate) },
              { label: 'Finance Co.',    value: fd.financeCompany || '—' },
            ].map(item => (
              <div key={item.label} className="bg-gray-50 rounded-xl p-3">
                <p className="text-xs text-gray-500">{item.label}</p>
                <p className="text-sm font-semibold text-gray-800 mt-0.5">{item.value}</p>
              </div>
            ))}
          </div>

          {/* EMI Schedule */}
          <div>
            <h3 className="font-bold text-sm text-gray-700 mb-2">EMI Schedule</h3>
            {loading ? (
              <div className="text-center py-6 text-gray-400 text-sm">Loading...</div>
            ) : (
              <div className="space-y-1.5 max-h-64 overflow-y-auto pr-1">
                {schedule.map(({ installment, dueDate, isPaid, isOverdue, payment }) => (
                  <div
                    key={installment}
                    className={`flex items-center justify-between px-3 py-2.5 rounded-xl border text-sm ${
                      isPaid     ? 'bg-green-50 border-green-100' :
                      isOverdue  ? 'bg-red-50 border-red-100' :
                      'bg-gray-50 border-gray-100'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      {isPaid    ? <CheckCircle2 size={15} className="text-green-600" /> :
                       isOverdue ? <XCircle size={15} className="text-red-500" /> :
                                   <Clock size={15} className="text-gray-400" />}
                      <span className="font-medium text-gray-700">#{installment}</span>
                      {dueDate && (
                        <span className="text-xs text-gray-400">Due: {fmt(dueDate.toISOString())}</span>
                      )}
                    </div>
                    <div className="flex items-center gap-2">
                      {isPaid && payment ? (
                        <>
                          <div className="text-right">
                            <p className="text-xs font-semibold text-green-700">{fmtINR(payment.amountPaid)}</p>
                            <p className="text-[10px] text-gray-400">{fmt(payment.paymentDate)} · {payment.paymentMode}</p>
                          </div>
                          <button
                            onClick={() => handleDelete(payment.id, installment)}
                            className="p-1 hover:bg-red-100 rounded text-red-500 transition-colors"
                            title="Delete this payment"
                          >
                            <Trash2 size={12} />
                          </button>
                        </>
                      ) : isOverdue ? (
                        <span className="text-xs font-bold text-red-600">Overdue</span>
                      ) : (
                        <span className="text-xs text-gray-400">Upcoming</span>
                      )}
                    </div>
                  </div>
                ))}
                {schedule.length === 0 && (
                  <p className="text-center text-sm text-gray-400 py-4">No schedule — loan start date not set</p>
                )}
              </div>
            )}
          </div>

          {/* Payment history list */}
          {payments.length > 0 && (
            <div>
              <h3 className="font-bold text-sm text-gray-700 mb-2">Payment Records</h3>
              <div className="space-y-2">
                {payments.map(p => (
                  <div key={p.id} className="flex items-start justify-between bg-white border border-gray-100 rounded-xl p-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <IndianRupee size={13} className="text-green-600" />
                        <span className="font-semibold text-sm text-gray-800">{fmtINR(p.amountPaid)}</span>
                        <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                          #{p.installmentNumber}
                        </span>
                        <span className="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded-full">
                          {p.paymentMode?.replace('_', ' ')}
                        </span>
                      </div>
                      <p className="text-xs text-gray-400 mt-0.5">
                        Paid: {fmt(p.paymentDate)}
                        {p.referenceNumber && ` · Ref: ${p.referenceNumber}`}
                        {p.recordedByName && ` · Recorded by: ${p.recordedByName}`}
                      </p>
                      {p.notes && <p className="text-xs text-gray-500 mt-0.5 italic">{p.notes}</p>}
                    </div>
                    <button
                      onClick={() => handleDelete(p.id, p.installmentNumber)}
                      className="p-1.5 hover:bg-red-50 rounded-lg text-red-400 transition-colors ml-2 flex-shrink-0"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="p-4 border-t border-gray-100 flex-shrink-0">
          <button onClick={onClose} className="w-full py-2.5 bg-gray-100 text-gray-700 rounded-xl text-sm font-semibold hover:bg-gray-200 transition-colors">
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
