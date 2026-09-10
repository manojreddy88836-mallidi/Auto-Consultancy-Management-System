import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-hot-toast';
import {
  Tag, Clock, CheckCircle2, XCircle, RefreshCw, ArrowDownUp,
  IndianRupee, Bike, MessageSquare, ChevronDown, ChevronUp,
  AlertCircle, Loader, RotateCcw
} from 'lucide-react';
import * as offerApi from '../../api/offerApi';

const BACKEND_BASE = 'http://localhost:8080';

// ── Status helpers ─────────────────────────────────────────────────────────
const STATUS_META = {
  PENDING:          { label: 'Pending',          color: 'bg-amber-100 text-amber-700',    icon: Clock },
  ACCEPTED:         { label: 'Accepted',          color: 'bg-green-100 text-green-700',    icon: CheckCircle2 },
  REJECTED:         { label: 'Rejected',          color: 'bg-red-100 text-red-700',        icon: XCircle },
  COUNTER_OFFER:    { label: 'Counter Offer',     color: 'bg-blue-100 text-blue-700',      icon: ArrowDownUp },
  COUNTER_ACCEPTED: { label: 'Counter Accepted',  color: 'bg-emerald-100 text-emerald-700',icon: CheckCircle2 },
  COUNTER_REJECTED: { label: 'Counter Rejected',  color: 'bg-rose-100 text-rose-700',      icon: XCircle },
  WITHDRAWN:        { label: 'Withdrawn',         color: 'bg-gray-100 text-gray-500',      icon: RotateCcw },
  EXPIRED:          { label: 'Expired',           color: 'bg-gray-100 text-gray-400',      icon: Clock },
};

const fmtINR = (v) =>
  v != null ? '₹' + Number(v).toLocaleString('en-IN', { minimumFractionDigits: 0 }) : '—';

const fmtDate = (d) =>
  d ? new Date(d).toLocaleDateString('en-IN', { day:'2-digit', month:'short', year:'numeric' }) : '—';

const StatusBadge = ({ status }) => {
  const m = STATUS_META[status] || { label: status, color: 'bg-gray-100 text-gray-600', icon: Tag };
  const Icon = m.icon;
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${m.color}`}>
      <Icon size={12} /> {m.label}
    </span>
  );
};

// ── Single Offer Card ──────────────────────────────────────────────────────
const OfferCard = ({ offer, onWithdraw, onCounterRespond, withdrawing, responding }) => {
  const [expanded, setExpanded] = useState(false);
  const imgSrc = offer.bikeImageUrl ? `${BACKEND_BASE}${offer.bikeImageUrl}` : null;
  const canWithdraw  = offer.status === 'PENDING' || offer.status === 'COUNTER_OFFER';
  const hasCounter   = offer.status === 'COUNTER_OFFER' && offer.counterOfferPrice != null;

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-all overflow-hidden">
      <div className="flex gap-0">
        {/* Bike thumbnail */}
        <div className="w-28 h-28 flex-shrink-0 bg-gradient-to-br from-slate-100 to-slate-200 relative">
          {imgSrc ? (
            <img src={imgSrc} alt={offer.modelName} className="w-full h-full object-cover" />
          ) : (
            <div className="absolute inset-0 flex items-center justify-center">
              <Bike size={32} className="text-gray-300" />
            </div>
          )}
        </div>

        {/* Main content */}
        <div className="flex-1 p-4 min-w-0">
          <div className="flex items-start justify-between gap-2 mb-2">
            <div>
              <p className="text-xs font-semibold text-[#1E88E5] uppercase tracking-wide">
                {offer.manufacturerName}
              </p>
              <h3 className="font-bold text-gray-900 text-sm leading-tight">
                {offer.modelName}{offer.variantName ? ` — ${offer.variantName}` : ''}
              </h3>
              {offer.registrationNumber && (
                <p className="text-xs text-gray-400 mt-0.5">{offer.registrationNumber}</p>
              )}
            </div>
            <StatusBadge status={offer.status} />
          </div>

          {/* Price grid */}
          <div className="grid grid-cols-3 gap-2 mb-2">
            <div className="bg-gray-50 rounded-lg p-2 text-center">
              <p className="text-[10px] text-gray-400 font-medium mb-0.5">Listed</p>
              <p className="text-xs font-bold text-gray-700">{fmtINR(offer.listedPrice)}</p>
            </div>
            <div className="bg-amber-50 rounded-lg p-2 text-center">
              <p className="text-[10px] text-amber-500 font-medium mb-0.5">My Offer</p>
              <p className="text-xs font-bold text-amber-700">{fmtINR(offer.offeredPrice)}</p>
            </div>
            {offer.counterOfferPrice != null ? (
              <div className="bg-blue-50 rounded-lg p-2 text-center">
                <p className="text-[10px] text-blue-500 font-medium mb-0.5">Counter</p>
                <p className="text-xs font-bold text-blue-700">{fmtINR(offer.counterOfferPrice)}</p>
              </div>
            ) : (
              <div className="rounded-lg p-2 text-center border border-dashed border-gray-200">
                <p className="text-[10px] text-gray-300 font-medium mb-0.5">Counter</p>
                <p className="text-xs font-bold text-gray-300">—</p>
              </div>
            )}
          </div>
          {/* Agreed price — shown when deal is closed */}
          {offer.agreedPrice != null && (
            <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200 rounded-xl px-3 py-2 mb-2">
              <span className="text-emerald-700 text-xs font-medium">✓ Negotiated / Agreed Price:</span>
              <span className="text-emerald-800 font-bold text-sm ml-auto">{fmtINR(offer.agreedPrice)}</span>
              <span className="text-emerald-500 text-[10px]">(Listed was {fmtINR(offer.listedPrice)})</span>
            </div>
          )}


          <div className="flex items-center justify-between">
            <p className="text-[10px] text-gray-400">Submitted {fmtDate(offer.createdAt)}</p>
            <button onClick={() => setExpanded(e => !e)}
              className="flex items-center gap-1 text-[10px] text-gray-400 hover:text-gray-600 transition-colors">
              {expanded ? <><ChevronUp size={12}/>Less</> : <><ChevronDown size={12}/>Details</>}
            </button>
          </div>
        </div>
      </div>

      {/* Expanded details */}
      {expanded && (
        <div className="border-t border-gray-100 p-4 space-y-3 bg-gray-50">
          {offer.customerMessage && (
            <div className="flex gap-2">
              <MessageSquare size={14} className="text-gray-400 mt-0.5 shrink-0" />
              <div>
                <p className="text-xs font-semibold text-gray-600 mb-0.5">Your message</p>
                <p className="text-xs text-gray-500">{offer.customerMessage}</p>
              </div>
            </div>
          )}
          {offer.adminResponse && (
            <div className="flex gap-2 bg-white rounded-xl p-3 border border-gray-100">
              <MessageSquare size={14} className="text-blue-400 mt-0.5 shrink-0" />
              <div>
                <p className="text-xs font-semibold text-blue-700 mb-0.5">
                  Response from {offer.respondedByName || 'Admin'}
                </p>
                <p className="text-xs text-gray-600">{offer.adminResponse}</p>
              </div>
            </div>
          )}
          <p className="text-[10px] text-gray-400">Last updated: {fmtDate(offer.updatedAt)}</p>

          {/* Counter offer action buttons */}
          {hasCounter && (
            <div className="border-t border-blue-100 pt-3">
              <p className="text-xs font-semibold text-blue-700 mb-2">
                Counter offer received: {fmtINR(offer.counterOfferPrice)}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => onCounterRespond(offer.id, true)}
                  disabled={responding}
                  className="flex-1 py-2 rounded-xl bg-green-500 hover:bg-green-600 text-white text-xs font-bold transition-all disabled:opacity-60 flex items-center justify-center gap-1">
                  {responding ? <Loader size={12} className="animate-spin" /> : <CheckCircle2 size={12} />}
                  Accept Counter ({fmtINR(offer.counterOfferPrice)})
                </button>
                <button
                  onClick={() => onCounterRespond(offer.id, false)}
                  disabled={responding}
                  className="flex-1 py-2 rounded-xl bg-red-500 hover:bg-red-600 text-white text-xs font-bold transition-all disabled:opacity-60 flex items-center justify-center gap-1">
                  <XCircle size={12} /> Reject Counter
                </button>
              </div>
            </div>
          )}

          {/* Withdraw */}
          {canWithdraw && !hasCounter && (
            <button
              onClick={() => onWithdraw(offer.id)}
              disabled={withdrawing}
              className="w-full py-2 rounded-xl border border-gray-300 text-gray-600 text-xs font-semibold hover:bg-white hover:border-red-300 hover:text-red-600 transition-all disabled:opacity-60 flex items-center justify-center gap-1.5">
              {withdrawing ? <Loader size={12} className="animate-spin" /> : <RotateCcw size={12} />}
              Withdraw Offer
            </button>
          )}
        </div>
      )}
    </div>
  );
};

// ── Page ───────────────────────────────────────────────────────────────────
export default function MyOffersPage() {
  const [offers,     setOffers]     = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [withdrawing, setWithdrawing] = useState(null);
  const [responding,  setResponding]  = useState(null);
  const [filter,     setFilter]     = useState('ALL');

  const load = useCallback(() => {
    setLoading(true);
    offerApi.getMyOffers()
      .then(r => setOffers(r.data?.data || []))
      .catch(() => toast.error('Failed to load offers'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleWithdraw = async (id) => {
    if (!window.confirm('Withdraw this offer?')) return;
    setWithdrawing(id);
    try {
      await offerApi.withdrawOffer(id);
      toast.success('Offer withdrawn');
      load();
    } catch (e) {
      toast.error(e?.response?.data?.message || 'Failed to withdraw');
    } finally { setWithdrawing(null); }
  };

  const handleCounterRespond = async (id, accept) => {
    setResponding(id);
    try {
      await offerApi.respondToCounter(id, accept);
      toast.success(accept ? 'Counter offer accepted!' : 'Counter offer rejected');
      load();
    } catch (e) {
      toast.error(e?.response?.data?.message || 'Failed to respond');
    } finally { setResponding(null); }
  };

  const FILTERS = ['ALL', 'PENDING', 'COUNTER_OFFER', 'ACCEPTED', 'COUNTER_ACCEPTED', 'REJECTED', 'WITHDRAWN'];
  const filtered = filter === 'ALL' ? offers : offers.filter(o => o.status === filter);

  return (
    <div className="max-w-3xl mx-auto py-6 px-4">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">My Offers</h1>
        <p className="text-gray-500 text-sm mt-1">
          Track your price negotiations on resale bikes
        </p>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 overflow-x-auto pb-2 mb-5 scrollbar-hide">
        {FILTERS.map(f => (
          <button key={f} onClick={() => setFilter(f)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all ${
              filter === f
                ? 'bg-[#1E88E5] text-white shadow-sm'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}>
            {f === 'ALL' ? 'All Offers' : (STATUS_META[f]?.label || f)}
            {f === 'ALL' && offers.length > 0 && (
              <span className="ml-1.5 bg-white/30 text-white rounded-full px-1.5 text-[10px]">
                {offers.length}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Content */}
      {loading ? (
        <div className="flex items-center justify-center py-24">
          <Loader size={28} className="animate-spin text-[#1E88E5]" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-20">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 mb-4">
            <Tag size={28} className="text-gray-300" />
          </div>
          <h3 className="text-gray-600 font-semibold mb-1">
            {filter === 'ALL' ? 'No offers yet' : `No ${STATUS_META[filter]?.label || filter} offers`}
          </h3>
          <p className="text-gray-400 text-sm">
            Browse available bikes and make an offer on any bike you like.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Counter offers notice */}
          {filtered.some(o => o.status === 'COUNTER_OFFER') && (
            <div className="bg-blue-50 border border-blue-200 rounded-xl px-4 py-3 flex items-center gap-2">
              <AlertCircle size={16} className="text-blue-600 shrink-0" />
              <p className="text-sm text-blue-700 font-medium">
                You have counter offers waiting for your response — scroll down to see them.
              </p>
            </div>
          )}

          {filtered.map(offer => (
            <OfferCard
              key={offer.id}
              offer={offer}
              onWithdraw={handleWithdraw}
              onCounterRespond={handleCounterRespond}
              withdrawing={withdrawing === offer.id}
              responding={responding === offer.id}
            />
          ))}
        </div>
      )}
    </div>
  );
}
