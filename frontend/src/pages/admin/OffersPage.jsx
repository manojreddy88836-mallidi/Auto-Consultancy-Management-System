import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-hot-toast';
import {
  Tag, Clock, CheckCircle2, XCircle, ArrowDownUp, Loader, Search,
  IndianRupee, Bike, MessageSquare, RefreshCw, ChevronDown, ChevronUp,
  RotateCcw, Send, Filter
} from 'lucide-react';
import * as offerApi from '../../api/offerApi';

const BACKEND_BASE = 'http://localhost:8080';

// ── Shared helpers ─────────────────────────────────────────────────────────
const STATUS_META = {
  PENDING:          { label: 'Pending',         color: 'bg-amber-100 text-amber-700 border-amber-200'   },
  ACCEPTED:         { label: 'Accepted',         color: 'bg-green-100 text-green-700 border-green-200'   },
  REJECTED:         { label: 'Rejected',         color: 'bg-red-100 text-red-700 border-red-200'         },
  COUNTER_OFFER:    { label: 'Counter Offer',    color: 'bg-blue-100 text-blue-700 border-blue-200'      },
  COUNTER_ACCEPTED: { label: 'Counter Accepted', color: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
  COUNTER_REJECTED: { label: 'Counter Rejected', color: 'bg-rose-100 text-rose-700 border-rose-200'     },
  WITHDRAWN:        { label: 'Withdrawn',        color: 'bg-gray-100 text-gray-500 border-gray-200'      },
  EXPIRED:          { label: 'Expired',          color: 'bg-gray-100 text-gray-400 border-gray-200'      },
};
const fmtINR  = (v) => v != null ? '₹' + Number(v).toLocaleString('en-IN') : '—';
const fmtDate = (d) => d ? new Date(d).toLocaleDateString('en-IN', { day:'2-digit', month:'short', year:'numeric' }) : '—';

const StatusBadge = ({ status }) => {
  const m = STATUS_META[status] || { label: status, color: 'bg-gray-100 text-gray-600 border-gray-200' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${m.color}`}>
      {m.label}
    </span>
  );
};

// ── Respond Modal ─────────────────────────────────────────────────────────
function RespondModal({ offer, onClose, onSubmit }) {
  const [action,        setAction]        = useState('ACCEPTED');
  const [counterPrice,  setCounterPrice]  = useState('');
  const [adminNote,     setAdminNote]     = useState('');
  const [submitting,    setSubmitting]    = useState(false);

  const handleSubmit = async () => {
    if (action === 'COUNTER_OFFER' && (!counterPrice || Number(counterPrice) <= 0)) {
      toast.error('Enter a valid counter offer price'); return;
    }
    setSubmitting(true);
    try {
      await onSubmit(offer.id, {
        action,
        counterOfferPrice: action === 'COUNTER_OFFER' ? parseFloat(counterPrice) : null,
        adminResponse: adminNote || null,
      });
      onClose();
    } finally { setSubmitting(false); }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6" onClick={e => e.stopPropagation()}>
        <h3 className="text-lg font-bold text-gray-900 mb-1">Respond to Offer</h3>
        <p className="text-sm text-gray-500 mb-5">
          {offer.customerName} offered {fmtINR(offer.offeredPrice)} for{' '}
          <strong>{offer.manufacturerName} {offer.modelName}</strong>
          {' '}(Listed: {fmtINR(offer.listedPrice)})
        </p>

        {/* Action selector */}
        <div className="space-y-2 mb-4">
          {[
            { val: 'ACCEPTED',      label: '✅ Accept the offer',              cls: 'border-green-300 bg-green-50 text-green-800' },
            { val: 'REJECTED',      label: '❌ Reject the offer',              cls: 'border-red-300 bg-red-50 text-red-800' },
            { val: 'COUNTER_OFFER', label: '💬 Send a counter offer',           cls: 'border-blue-300 bg-blue-50 text-blue-800' },
          ].map(opt => (
            <label key={opt.val}
              className={`flex items-center gap-3 p-3 rounded-xl border-2 cursor-pointer transition-all ${
                action === opt.val ? opt.cls : 'border-gray-200 hover:border-gray-300'
              }`}>
              <input type="radio" checked={action === opt.val} onChange={() => setAction(opt.val)}
                className="w-4 h-4 accent-[#1E88E5]" />
              <span className="text-sm font-medium">{opt.label}</span>
            </label>
          ))}
        </div>

        {/* Counter price input */}
        {action === 'COUNTER_OFFER' && (
          <div className="mb-4">
            <label className="block text-sm font-semibold text-gray-700 mb-1.5">
              Counter Offer Price <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 font-medium text-sm">₹</span>
              <input
                type="number" value={counterPrice} onChange={e => setCounterPrice(e.target.value)}
                placeholder="e.g. 95000" min={1}
                className="w-full pl-8 pr-4 py-2.5 rounded-xl border border-gray-200 focus:border-[#1E88E5] focus:ring-2 focus:ring-blue-500/20 outline-none text-sm" />
            </div>
          </div>
        )}

        {/* Note */}
        <div className="mb-5">
          <label className="block text-sm font-semibold text-gray-700 mb-1.5">Note to customer (optional)</label>
          <textarea value={adminNote} onChange={e => setAdminNote(e.target.value)}
            rows={2} placeholder="Reason or additional message..."
            className="w-full px-3 py-2.5 rounded-xl border border-gray-200 focus:border-[#1E88E5] focus:ring-2 focus:ring-blue-500/20 outline-none text-sm resize-none" />
        </div>

        <div className="flex gap-3">
          <button onClick={onClose}
            className="flex-1 py-2.5 rounded-xl border border-gray-200 text-gray-600 text-sm font-medium hover:bg-gray-50 transition-all">
            Cancel
          </button>
          <button onClick={handleSubmit} disabled={submitting}
            className="flex-1 py-2.5 rounded-xl bg-[#0F1B35] hover:bg-[#1E3A5F] text-white text-sm font-bold transition-all disabled:opacity-60 flex items-center justify-center gap-2">
            {submitting ? <Loader size={14} className="animate-spin" /> : <Send size={14} />}
            Submit Response
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Offer Row ─────────────────────────────────────────────────────────────
function OfferRow({ offer, onRespond }) {
  const [expanded, setExpanded] = useState(false);
  const imgSrc = offer.bikeImageUrl ? `${BACKEND_BASE}${offer.bikeImageUrl}` : null;
  const canRespond = offer.status === 'PENDING' || offer.status === 'COUNTER_OFFER';

  return (
    <>
      <tr className="hover:bg-gray-50 transition-colors cursor-pointer" onClick={() => setExpanded(e => !e)}>
        {/* Bike */}
        <td className="px-4 py-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
              {imgSrc
                ? <img src={imgSrc} alt="" className="w-full h-full object-cover" />
                : <div className="w-full h-full flex items-center justify-center"><Bike size={16} className="text-gray-300" /></div>}
            </div>
            <div>
              <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide">{offer.manufacturerName}</p>
              <p className="text-sm font-bold text-gray-800">{offer.modelName}</p>
              {offer.registrationNumber && <p className="text-xs text-gray-400">{offer.registrationNumber}</p>}
            </div>
          </div>
        </td>
        {/* Customer */}
        <td className="px-4 py-3">
          <p className="text-sm font-semibold text-gray-800">{offer.customerName}</p>
          <p className="text-xs text-gray-400">{offer.customerEmail}</p>
        </td>
        {/* Prices */}
        <td className="px-4 py-3">
          <div className="space-y-0.5">
            <p className="text-xs text-gray-400">Listed: <span className="font-medium text-gray-600">{fmtINR(offer.listedPrice)}</span></p>
            <p className="text-xs text-amber-600 font-semibold">Offer: {fmtINR(offer.offeredPrice)}</p>
            {offer.counterOfferPrice != null && (
              <p className="text-xs text-blue-600 font-semibold">Counter: {fmtINR(offer.counterOfferPrice)}</p>
            )}
            {offer.agreedPrice != null && (
              <p className="text-xs text-emerald-700 font-bold bg-emerald-50 px-1.5 py-0.5 rounded inline-block">✓ Agreed: {fmtINR(offer.agreedPrice)}</p>
            )}
          </div>
        </td>
        {/* Status */}
        <td className="px-4 py-3"><StatusBadge status={offer.status} /></td>
        {/* Date */}
        <td className="px-4 py-3 text-xs text-gray-400">{fmtDate(offer.createdAt)}</td>
        {/* Action */}
        <td className="px-4 py-3">
          <div className="flex items-center gap-2">
            {canRespond && (
              <button onClick={e => { e.stopPropagation(); onRespond(offer); }}
                className="px-3 py-1.5 rounded-lg bg-[#1E88E5] hover:bg-[#1976D2] text-white text-xs font-semibold transition-all">
                Respond
              </button>
            )}
            {expanded ? <ChevronUp size={14} className="text-gray-400" /> : <ChevronDown size={14} className="text-gray-400" />}
          </div>
        </td>
      </tr>
      {expanded && (
        <tr>
          <td colSpan={6} className="px-4 pb-4 bg-gray-50">
            <div className="rounded-xl border border-gray-200 p-4 space-y-2 bg-white">
              {offer.customerMessage && (
                <div>
                  <p className="text-xs font-semibold text-gray-500 mb-0.5">Customer message:</p>
                  <p className="text-sm text-gray-700">{offer.customerMessage}</p>
                </div>
              )}
              {offer.adminResponse && (
                <div>
                  <p className="text-xs font-semibold text-blue-600 mb-0.5">Admin/Worker response:</p>
                  <p className="text-sm text-gray-700">{offer.adminResponse}</p>
                </div>
              )}
              {offer.respondedByName && (
                <p className="text-xs text-gray-400">Responded by: {offer.respondedByName}</p>
              )}
              <p className="text-xs text-gray-400">Updated: {fmtDate(offer.updatedAt)}</p>
              <p className="text-xs text-gray-400">Bike ID: #{offer.bikeInventoryId}</p>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────
export default function AdminOffersPage() {
  const [offers,    setOffers]    = useState([]);
  const [loading,   setLoading]   = useState(true);
  const [page,      setPage]      = useState(0);
  const [totalPages,setTotalPages]= useState(1);
  const [status,    setStatus]    = useState('');
  const [responding,setResponding]= useState(null); // offer being responded to

  const load = useCallback(() => {
    setLoading(true);
    offerApi.getAllOffers({ page, size: 15, status: status || undefined })
      .then(r => {
        const d = r.data?.data;
        setOffers(d?.content || []);
        setTotalPages(d?.totalPages || 1);
      })
      .catch(() => toast.error('Failed to load offers'))
      .finally(() => setLoading(false));
  }, [page, status]);

  useEffect(() => { load(); }, [load]);

  const handleRespond = async (offerId, data) => {
    try {
      await offerApi.respondToOffer(offerId, data);
      toast.success('Response sent to customer');
      load();
    } catch (e) {
      toast.error(e?.response?.data?.message || 'Failed to respond');
      throw e;
    }
  };

  const STATUS_FILTERS = ['', 'PENDING', 'COUNTER_OFFER', 'ACCEPTED', 'COUNTER_ACCEPTED', 'REJECTED', 'WITHDRAWN'];
  const pendingCount = offers.filter(o => o.status === 'PENDING' || o.status === 'COUNTER_OFFER').length;

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Price Offers</h1>
          <p className="text-sm text-gray-500 mt-0.5">Customer bargain / offer negotiations</p>
        </div>
        <button onClick={load} className="p-2 rounded-xl border border-gray-200 text-gray-500 hover:bg-gray-50 transition-all">
          <RefreshCw size={16} />
        </button>
      </div>

      {/* Pending alert */}
      {pendingCount > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 flex items-center gap-2">
          <Clock size={15} className="text-amber-600 shrink-0" />
          <p className="text-sm text-amber-700 font-medium">
            {pendingCount} offer{pendingCount > 1 ? 's' : ''} awaiting your response
          </p>
        </div>
      )}

      {/* Filter bar */}
      <div className="flex gap-2 flex-wrap items-center">
        <Filter size={14} className="text-gray-400" />
        {STATUS_FILTERS.map(s => (
          <button key={s} onClick={() => { setStatus(s); setPage(0); }}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${
              status === s
                ? 'bg-[#0F1B35] text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}>
            {s === '' ? 'All' : (STATUS_META[s]?.label || s)}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader size={28} className="animate-spin text-[#1E88E5]" />
          </div>
        ) : offers.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <Tag size={32} className="mx-auto mb-3 opacity-30" />
            <p className="font-medium">No offers found</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50">
                    {['Bike', 'Customer', 'Prices', 'Status', 'Date', 'Action'].map(h => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {offers.map(o => (
                    <OfferRow key={o.id} offer={o} onRespond={setResponding} />
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="px-4 py-3 border-t border-gray-100 flex items-center justify-between">
                <p className="text-xs text-gray-500">Page {page + 1} of {totalPages}</p>
                <div className="flex gap-2">
                  <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                    className="px-3 py-1.5 rounded-lg border border-gray-200 text-xs font-medium disabled:opacity-40">← Prev</button>
                  <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                    className="px-3 py-1.5 rounded-lg border border-gray-200 text-xs font-medium disabled:opacity-40">Next →</button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Respond modal */}
      {responding && (
        <RespondModal
          offer={responding}
          onClose={() => setResponding(null)}
          onSubmit={handleRespond}
        />
      )}
    </div>
  );
}
