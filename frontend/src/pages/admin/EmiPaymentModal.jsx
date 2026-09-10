import React, { useState, useEffect, useMemo } from 'react';
import { X, CreditCard, Calendar, IndianRupee, CheckCircle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { recordEmiPayment } from '../../api/emiPaymentApi';
import { fmtINR } from '../../utils/emiCalculator';

const PAYMENT_MODES = ['CASH', 'UPI', 'BANK_TRANSFER', 'CARD', 'OTHER'];

/**
 * Modal for Admin to record an EMI payment against a financed application.
 *
 * Props:
 *  record       – ApplicationResponse object (has financeDetailId, emiAmount, numberOfEmis, etc.)
 *  paidSet      – Set<number> of already-paid installment numbers (from EmiPayment API)
 *  onClose      – fn()
 *  onSuccess    – fn() — called after successful payment to refresh parent
 */
export default function EmiPaymentModal({ record, paidSet = new Set(), onClose, onSuccess }) {
  const [form, setForm] = useState({
    installmentNumber: '',
    paymentDate: new Date().toISOString().split('T')[0],
    amountPaid: record.emiAmount ? String(record.emiAmount) : '',
    paymentMode: 'CASH',
    referenceNumber: '',
    notes: '',
  });
  const [saving, setSaving] = useState(false);

  // Build list of unpaid installments
  const unpaidInstallments = useMemo(() => {
    const total = record.numberOfEmis || 0;
    const list = [];
    for (let i = 1; i <= total; i++) {
      if (!paidSet.has(i)) list.push(i);
    }
    return list;
  }, [record.numberOfEmis, paidSet]);

  // Auto-select first unpaid installment
  useEffect(() => {
    if (unpaidInstallments.length > 0 && !form.installmentNumber) {
      setForm(f => ({ ...f, installmentNumber: String(unpaidInstallments[0]) }));
    }
  }, [unpaidInstallments]);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.installmentNumber) return toast.error('Select an installment number');
    if (!form.paymentDate)       return toast.error('Enter payment date');
    if (!form.amountPaid || Number(form.amountPaid) <= 0) return toast.error('Enter a valid amount');

    setSaving(true);
    try {
      await recordEmiPayment({
        financeDetailId:   record.financeDetailId,
        installmentNumber: Number(form.installmentNumber),
        paymentDate:       form.paymentDate,
        amountPaid:        Number(form.amountPaid),
        paymentMode:       form.paymentMode,
        referenceNumber:   form.referenceNumber || null,
        notes:             form.notes || null,
      });
      toast.success(`EMI #${form.installmentNumber} recorded successfully`);
      onSuccess?.();
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to record payment');
    } finally {
      setSaving(false);
    }
  };

  const inputCls = 'w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none';

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg">

        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <CreditCard size={18} className="text-blue-600" />
            <h2 className="font-bold text-gray-900">Record EMI Payment</h2>
          </div>
          <button onClick={onClose} className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors">
            <X size={16} />
          </button>
        </div>

        {/* Loan summary */}
        <div className="mx-5 mt-4 mb-2 bg-blue-50 rounded-xl p-3 grid grid-cols-3 gap-2 text-center">
          <div>
            <p className="text-xs text-gray-500">Monthly EMI</p>
            <p className="text-sm font-bold text-blue-700">{fmtINR(record.emiAmount)}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500">Paid / Total</p>
            <p className="text-sm font-bold text-gray-800">{paidSet.size} / {record.numberOfEmis || 0}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500">Outstanding</p>
            <p className="text-sm font-bold text-orange-600">{fmtINR(record.outstandingLoanAmount)}</p>
          </div>
        </div>

        {unpaidInstallments.length === 0 ? (
          <div className="p-8 text-center">
            <CheckCircle size={40} className="text-green-500 mx-auto mb-3" />
            <p className="font-semibold text-gray-700">All EMIs are fully paid!</p>
            <p className="text-sm text-gray-500 mt-1">No unpaid installments remain.</p>
            <button onClick={onClose} className="mt-4 px-4 py-2 bg-gray-100 rounded-xl text-sm font-medium">Close</button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-5 space-y-4">

            {/* Installment selector */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                Installment Number <span className="text-red-500">*</span>
              </label>
              <select
                value={form.installmentNumber}
                onChange={e => set('installmentNumber', e.target.value)}
                className={inputCls}
                required
              >
                <option value="">Select installment</option>
                {unpaidInstallments.map(n => (
                  <option key={n} value={n}>#{n} of {record.numberOfEmis}</option>
                ))}
              </select>
            </div>

            {/* Payment date */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                <Calendar size={13} className="inline mr-1" /> Payment Date <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                value={form.paymentDate}
                onChange={e => set('paymentDate', e.target.value)}
                className={inputCls}
                max={new Date().toISOString().split('T')[0]}
                required
              />
            </div>

            {/* Amount paid */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                <IndianRupee size={13} className="inline mr-1" /> Amount Paid <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                value={form.amountPaid}
                onChange={e => set('amountPaid', e.target.value)}
                className={inputCls}
                min="1"
                step="0.01"
                required
              />
              <p className="text-xs text-gray-400 mt-0.5">Standard EMI: {fmtINR(record.emiAmount)}</p>
            </div>

            {/* Payment mode */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Payment Mode</label>
              <div className="flex flex-wrap gap-2">
                {PAYMENT_MODES.map(m => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => set('paymentMode', m)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                      form.paymentMode === m
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {m.replace('_', ' ')}
                  </button>
                ))}
              </div>
            </div>

            {/* Reference number */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                Reference / Transaction ID <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <input
                type="text"
                value={form.referenceNumber}
                onChange={e => set('referenceNumber', e.target.value)}
                placeholder="e.g. UPI ref, cheque no..."
                className={inputCls}
              />
            </div>

            {/* Notes */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                Notes <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <textarea
                value={form.notes}
                onChange={e => set('notes', e.target.value)}
                rows={2}
                placeholder="Any remarks..."
                className={inputCls + ' resize-none'}
              />
            </div>

            {/* Actions */}
            <div className="flex gap-3 pt-1">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 bg-gray-100 text-gray-700 rounded-xl text-sm font-semibold hover:bg-gray-200 transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="flex-1 py-2.5 bg-blue-600 text-white rounded-xl text-sm font-semibold hover:bg-blue-700 transition-colors disabled:opacity-50"
              >
                {saving ? 'Saving...' : 'Record Payment'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
