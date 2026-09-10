import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, Bike, ChevronLeft, ChevronRight, ZoomIn, X,
  CheckCircle, Tag, Send, Loader, RotateCcw, ArrowDownUp,
  MessageSquare, AlertCircle, IndianRupee
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import { getPublicBikeDetail } from '../../api/bikeInventoryApi';
import * as offerApi from '../../api/offerApi';
import useAuth from '../../hooks/useAuth';

const BACKEND_BASE = 'http://localhost:8080';
const imgUrl = (url) => url ? `${BACKEND_BASE}${url}` : null;
const fmtINR = (v) => v != null ? '₹' + Number(v).toLocaleString('en-IN') : '—';

// ── Status display helper ─────────────────────────────────────────────────
const STATUS_META = {
  PENDING:          { label: 'Pending Review',    color: 'text-amber-600 bg-amber-50 border-amber-200' },
  ACCEPTED:         { label: 'Accepted ✓',         color: 'text-green-600 bg-green-50 border-green-200' },
  REJECTED:         { label: 'Rejected',           color: 'text-red-600 bg-red-50 border-red-200' },
  COUNTER_OFFER:    { label: 'Counter Offer',      color: 'text-blue-600 bg-blue-50 border-blue-200' },
  COUNTER_ACCEPTED: { label: 'Counter Accepted ✓', color: 'text-emerald-600 bg-emerald-50 border-emerald-200' },
  COUNTER_REJECTED: { label: 'Counter Rejected',   color: 'text-rose-600 bg-rose-50 border-rose-200' },
  WITHDRAWN:        { label: 'Withdrawn',          color: 'text-gray-400 bg-gray-50 border-gray-200' },
  EXPIRED:          { label: 'Expired',            color: 'text-gray-400 bg-gray-50 border-gray-200' },
};

// ── Offer Panel ───────────────────────────────────────────────────────────
function OfferPanel({ bike, currentOffer, onOfferSubmitted }) {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [amount,      setAmount]      = useState('');
  const [message,     setMessage]     = useState('');
  const [submitting,  setSubmitting]  = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);
  const [responding,  setResponding]  = useState(false);
  const [showForm,    setShowForm]    = useState(false);

  // Has an existing active offer?
  const hasActiveOffer = currentOffer &&
    ['PENDING', 'COUNTER_OFFER'].includes(currentOffer.status);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!user) { navigate('/login'); return; }
    const val = parseFloat(amount);
    if (!val || val <= 0) { toast.error('Enter a valid offer amount'); return; }
    setSubmitting(true);
    try {
      await offerApi.submitOffer({
        bikeInventoryId: bike.id,
        offeredPrice: val,
        customerMessage: message.trim() || null,
      });
      toast.success('Your offer has been submitted!');
      setAmount(''); setMessage(''); setShowForm(false);
      onOfferSubmitted();
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to submit offer');
    } finally { setSubmitting(false); }
  };

  const handleWithdraw = async () => {
    if (!window.confirm('Withdraw your current offer?')) return;
    setWithdrawing(true);
    try {
      await offerApi.withdrawOffer(currentOffer.id);
      toast.success('Offer withdrawn');
      onOfferSubmitted();
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to withdraw');
    } finally { setWithdrawing(false); }
  };

  const handleCounterRespond = async (accept) => {
    setResponding(true);
    try {
      await offerApi.respondToCounter(currentOffer.id, accept);
      toast.success(accept ? 'Counter offer accepted!' : 'Counter offer rejected');
      onOfferSubmitted();
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to respond');
    } finally { setResponding(false); }
  };

  const statusMeta = currentOffer ? (STATUS_META[currentOffer.status] || {}) : null;

  return (
    <div className="border border-gray-200 rounded-2xl overflow-hidden bg-gradient-to-b from-white to-gray-50">
      {/* Header */}
      <div className="px-4 py-3 bg-white border-b border-gray-100 flex items-center gap-2">
        <Tag size={16} className="text-[#1E88E5]" />
        <span className="font-bold text-gray-800 text-sm">Make an Offer</span>
        <span className="ml-auto text-xs text-gray-400 italic">Bargain on price</span>
      </div>

      <div className="p-4 space-y-4">
        {/* Listed price display (always read-only) */}
        <div className="flex items-center justify-between bg-gray-100 rounded-xl px-4 py-3">
          <span className="text-sm text-gray-500 font-medium">Listed Price</span>
          <span className="text-base font-extrabold text-gray-800">{fmtINR(bike.price)}</span>
        </div>

        {/* If not logged in */}
        {!user && (
          <div className="text-center py-3">
            <p className="text-sm text-gray-500 mb-3">Login to make an offer</p>
            <button onClick={() => navigate('/login')}
              className="px-5 py-2 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white text-sm font-semibold transition-all">
              Login to Offer
            </button>
          </div>
        )}

        {/* Active offer status display */}
        {user && currentOffer && (
          <div className={`rounded-xl border p-3 ${statusMeta.color}`}>
            <div className="flex items-start justify-between gap-2 mb-2">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide opacity-70">Current Offer Status</p>
                <p className="font-bold text-sm">{statusMeta.label}</p>
              </div>
              <div className="text-right">
                <p className="text-xs opacity-70">Your offer</p>
                <p className="font-extrabold">{fmtINR(currentOffer.offeredPrice)}</p>
              </div>
            </div>

            {/* Counter offer details */}
            {currentOffer.counterOfferPrice && (
              <div className="bg-white/60 rounded-lg p-2 mt-2 mb-2">
                <p className="text-xs font-semibold mb-0.5">Counter offer from seller</p>
                <p className="text-lg font-extrabold">{fmtINR(currentOffer.counterOfferPrice)}</p>
                {currentOffer.adminResponse && (
                  <p className="text-xs mt-1 opacity-80">"{currentOffer.adminResponse}"</p>
                )}
              </div>
            )}

            {/* Response note */}
            {currentOffer.adminResponse && !currentOffer.counterOfferPrice && (
              <p className="text-xs mt-1 opacity-80">"{currentOffer.adminResponse}"</p>
            )}

            {/* Counter offer action buttons */}
            {currentOffer.status === 'COUNTER_OFFER' && currentOffer.counterOfferPrice && (
              <div className="flex gap-2 mt-3">
                <button onClick={() => handleCounterRespond(true)} disabled={responding}
                  className="flex-1 py-2 rounded-lg bg-green-500 hover:bg-green-600 text-white text-xs font-bold transition-all disabled:opacity-60 flex items-center justify-center gap-1">
                  {responding ? <Loader size={12} className="animate-spin" /> : <CheckCircle size={12} />}
                  Accept {fmtINR(currentOffer.counterOfferPrice)}
                </button>
                <button onClick={() => handleCounterRespond(false)} disabled={responding}
                  className="flex-1 py-2 rounded-lg bg-red-500 hover:bg-red-600 text-white text-xs font-bold transition-all disabled:opacity-60 flex items-center justify-center gap-1">
                  <X size={12} /> Reject
                </button>
              </div>
            )}

            {/* Withdraw button for pending */}
            {(currentOffer.status === 'PENDING') && (
              <button onClick={handleWithdraw} disabled={withdrawing}
                className="w-full mt-2 py-1.5 rounded-lg border border-current text-xs font-semibold hover:opacity-80 transition-all disabled:opacity-40 flex items-center justify-center gap-1.5">
                {withdrawing ? <Loader size={11} className="animate-spin" /> : <RotateCcw size={11} />}
                Withdraw Offer
              </button>
            )}

            {/* Make new offer after terminal status */}
            {['ACCEPTED', 'REJECTED', 'COUNTER_ACCEPTED', 'COUNTER_REJECTED', 'WITHDRAWN', 'EXPIRED'].includes(currentOffer.status) && (
              <button onClick={() => setShowForm(true)}
                className="w-full mt-2 py-1.5 rounded-lg border border-current text-xs font-semibold hover:opacity-80 transition-all">
                Make a New Offer
              </button>
            )}
          </div>
        )}

        {/* Offer form — show when no active offer, or user explicitly clicked new offer */}
        {user && bike.saleStatus === 'AVAILABLE' && (!currentOffer || showForm) && !hasActiveOffer && (
          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1.5">
                Your Offer Price <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 font-semibold text-sm">₹</span>
                <input
                  type="number"
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                  placeholder={bike.price ? `Max ${fmtINR(bike.price)}` : 'Enter amount'}
                  min={1}
                  step={100}
                  required
                  className="w-full pl-8 pr-4 py-2.5 rounded-xl border border-gray-200 focus:border-[#1E88E5] focus:ring-2 focus:ring-blue-500/20 outline-none text-sm font-medium"
                />
              </div>
              {amount && bike.price && Number(amount) < Number(bike.price) && (
                <p className="text-xs text-amber-600 mt-1">
                  Your offer is {fmtINR(Number(bike.price) - Number(amount))} below listed price
                </p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1.5">
                Message (optional)
              </label>
              <textarea
                value={message}
                onChange={e => setMessage(e.target.value)}
                placeholder="Why this price? Any specific reason..."
                rows={2}
                maxLength={300}
                className="w-full px-3 py-2.5 rounded-xl border border-gray-200 focus:border-[#1E88E5] focus:ring-2 focus:ring-blue-500/20 outline-none text-sm resize-none"
              />
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full py-3 rounded-xl bg-[#0F1B35] hover:bg-[#1E3A5F] text-white font-bold text-sm transition-all disabled:opacity-60 flex items-center justify-center gap-2 shadow-md"
            >
              {submitting ? <Loader size={16} className="animate-spin" /> : <Send size={16} />}
              Send Offer
            </button>

            <p className="text-[10px] text-gray-400 text-center leading-relaxed">
              Submitting an offer does not reserve or purchase this bike. The listed price
              of <strong>{fmtINR(bike.price)}</strong> remains unchanged until a deal is finalized.
            </p>
          </form>
        )}

        {/* Bike sold/reserved — no offer possible */}
        {user && bike.saleStatus !== 'AVAILABLE' && !currentOffer && (
          <div className="text-center py-3 text-gray-400">
            <AlertCircle size={20} className="mx-auto mb-1.5" />
            <p className="text-xs">Offers not available — bike is {bike.saleStatus?.toLowerCase()}</p>
          </div>
        )}
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────
export default function BikeDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [bike,        setBike]        = useState(null);
  const [loading,     setLoading]     = useState(true);
  const [selectedImg, setSelectedImg] = useState(0);
  const [lightbox,    setLightbox]    = useState(false);
  const [applying,    setApplying]    = useState(false);
  const [myOffer,     setMyOffer]     = useState(null);
  const [offerLoading,setOfferLoading]= useState(false);

  useEffect(() => {
    setLoading(true);
    getPublicBikeDetail(id)
      .then(r => { setBike(r.data?.data); setSelectedImg(0); })
      .catch(() => toast.error('Failed to load bike details'))
      .finally(() => setLoading(false));
  }, [id]);

  const loadMyOffer = useCallback(() => {
    if (!user || user.role !== 'CUSTOMER') return;
    setOfferLoading(true);
    offerApi.getMyOfferForBike(id)
      .then(r => setMyOffer(r.data?.data || null))
      .catch(() => {})
      .finally(() => setOfferLoading(false));
  }, [id, user]);

  useEffect(() => { loadMyOffer(); }, [loadMyOffer]);

  const images = bike?.images || [];
  const hasPrimary = images.findIndex(i => i.primary);
  const orderedImages = hasPrimary > 0
    ? [images[hasPrimary], ...images.filter((_, i) => i !== hasPrimary)]
    : images;

  const prev = () => setSelectedImg(i => (i - 1 + orderedImages.length) % orderedImages.length);
  const next = () => setSelectedImg(i => (i + 1) % orderedImages.length);

  const handleApply = () => {
    if (!user) { navigate('/login'); return; }
    if (bike?.saleStatus !== 'AVAILABLE') { toast.error('This bike is not available for sale'); return; }
    navigate('/customer/submit', {
      state: { selectedBike: bike, bikeInventoryId: bike.id, bikeModelId: bike.bikeModelId }
    });
  };

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-10 h-10 border-4 border-[#1E88E5] border-t-transparent rounded-full animate-spin" />
    </div>
  );
  if (!bike) return (
    <div className="text-center py-24 text-gray-400">
      <Bike size={48} className="mx-auto mb-3 opacity-20" />
      <p>Bike not found</p>
    </div>
  );

  const mainImgSrc = orderedImages[selectedImg] ? imgUrl(orderedImages[selectedImg].imageUrl) : null;
  const engineCC = bike.variants?.find(v => v.engineCC)?.engineCC;

  return (
    <>
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Back */}
        <button onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-gray-500 hover:text-gray-800 text-sm font-medium transition-colors">
          <ArrowLeft size={16} /> Back to Bikes
        </button>

        <div className="grid lg:grid-cols-2 gap-8">
          {/* Image gallery */}
          <div className="space-y-3">
            <div className="relative rounded-2xl overflow-hidden bg-gradient-to-br from-slate-100 to-slate-200 aspect-[4/3] group cursor-pointer"
                 onClick={() => orderedImages.length > 0 && setLightbox(true)}>
              {mainImgSrc ? (
                <img src={mainImgSrc} alt={bike.modelName} className="w-full h-full object-cover" loading="lazy" />
              ) : (
                <div className="absolute inset-0 flex flex-col items-center justify-center text-gray-400">
                  <Bike size={64} className="opacity-20 mb-3" />
                  <span className="text-sm opacity-50">No image available</span>
                </div>
              )}
              {orderedImages.length > 1 && (
                <>
                  <button onClick={e => { e.stopPropagation(); prev(); }}
                    className="absolute left-3 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white rounded-full p-2 shadow transition-all">
                    <ChevronLeft size={18} />
                  </button>
                  <button onClick={e => { e.stopPropagation(); next(); }}
                    className="absolute right-3 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white rounded-full p-2 shadow transition-all">
                    <ChevronRight size={18} />
                  </button>
                </>
              )}
              {orderedImages.length > 0 && (
                <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity bg-black/40 text-white rounded-full p-1.5">
                  <ZoomIn size={14} />
                </div>
              )}
              {orderedImages.length > 1 && (
                <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5">
                  {orderedImages.map((_, i) => (
                    <button key={i} onClick={e => { e.stopPropagation(); setSelectedImg(i); }}
                      className={`w-2 h-2 rounded-full transition-all ${i === selectedImg ? 'bg-white scale-125' : 'bg-white/50'}`} />
                  ))}
                </div>
              )}
            </div>

            {orderedImages.length > 1 && (
              <div className="flex gap-2 overflow-x-auto pb-1">
                {orderedImages.map((img, i) => (
                  <button key={img.id} onClick={() => setSelectedImg(i)}
                    className={`shrink-0 w-16 h-16 rounded-xl overflow-hidden border-2 transition-all ${i === selectedImg ? 'border-[#1E88E5] shadow-md' : 'border-gray-200 opacity-70'}`}>
                    <img src={imgUrl(img.imageUrl)} alt="" className="w-full h-full object-cover" loading="lazy" />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Bike info + CTAs */}
          <div className="space-y-5">
            <div>
              <p className="text-sm font-bold text-[#1E88E5] uppercase tracking-wide">{bike.manufacturerName}</p>
              <h1 className="text-3xl font-extrabold text-gray-900 mt-1">{bike.modelName}</h1>
              {bike.price && (
                <p className="text-2xl font-bold text-gray-800 mt-2">
                  ₹ {Number(bike.price).toLocaleString('en-IN')}
                </p>
              )}
            </div>

            {/* Status badge */}
            <div className="flex items-center gap-2">
              <span className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-semibold
                ${bike.saleStatus === 'AVAILABLE' ? 'bg-green-100 text-green-700' :
                  bike.saleStatus === 'RESERVED' ? 'bg-amber-100 text-amber-700' :
                  bike.saleStatus === 'SOLD' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600'}`}>
                <CheckCircle size={14} />
                {bike.saleStatus}
              </span>
            </div>

            {/* Specs grid */}
            <div className="grid grid-cols-2 gap-3">
              {[
                { label: 'Brand',    value: bike.manufacturerName },
                { label: 'Category', value: bike.category },
                { label: 'Fuel Type',value: bike.fuelType },
                { label: 'Engine',   value: engineCC ? `${engineCC} CC` : null },
                { label: 'Variants', value: bike.variants?.length ? `${bike.variants.length} variant${bike.variants.length > 1 ? 's' : ''}` : null },
              ].filter(s => s.value).map(spec => (
                <div key={spec.label} className="bg-gray-50 rounded-xl p-3">
                  <p className="text-xs text-gray-500 mb-0.5">{spec.label}</p>
                  <p className="text-sm font-semibold text-gray-800">{spec.value}</p>
                </div>
              ))}
            </div>

            {/* Variants */}
            {bike.variants?.length > 0 && (
              <div>
                <p className="text-sm font-semibold text-gray-700 mb-2">Available Variants</p>
                <div className="flex flex-wrap gap-2">
                  {bike.variants.map(v => (
                    <span key={v.id} className="px-3 py-1 bg-blue-50 text-blue-700 text-xs font-medium rounded-full">
                      {v.variantName}{v.engineCC ? ` · ${v.engineCC}CC` : ''}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Apply button */}
            <div className="pt-4 border-t border-gray-100">
              {bike.saleStatus === 'AVAILABLE' ? (
                <button onClick={handleApply} disabled={applying}
                  className="w-full py-4 rounded-2xl bg-[#1E88E5] hover:bg-[#1976D2] text-white font-bold text-lg shadow-lg shadow-blue-200 transition-all disabled:opacity-70">
                  {applying ? 'Creating Application…' : '🏍️ Apply for This Bike'}
                </button>
              ) : (
                <div className="w-full py-4 rounded-2xl bg-gray-100 text-gray-400 font-semibold text-center">
                  {bike.saleStatus === 'RESERVED' ? 'This bike is reserved' :
                   bike.saleStatus === 'SOLD' ? 'This bike has been sold' : 'Not available for sale'}
                </div>
              )}
            </div>

            {/* ── Make an Offer section ─────────────────────────────────── */}
            {user?.role === 'CUSTOMER' || !user ? (
              <OfferPanel
                bike={bike}
                currentOffer={offerLoading ? null : myOffer}
                onOfferSubmitted={loadMyOffer}
              />
            ) : null}
            {/* Non-customer logged-in users (admin/worker) don't see offer panel */}
          </div>
        </div>
      </div>

      {/* Lightbox */}
      {lightbox && orderedImages.length > 0 && (
        <div className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center" onClick={() => setLightbox(false)}>
          <button className="absolute top-4 right-4 text-white/80 hover:text-white" onClick={() => setLightbox(false)}>
            <X size={28} />
          </button>
          <button className="absolute left-4 top-1/2 -translate-y-1/2 text-white/80 hover:text-white"
            onClick={e => { e.stopPropagation(); prev(); }}>
            <ChevronLeft size={36} />
          </button>
          <img src={imgUrl(orderedImages[selectedImg].imageUrl)} alt=""
            className="max-w-[90vw] max-h-[90vh] object-contain rounded-xl"
            onClick={e => e.stopPropagation()} />
          <button className="absolute right-4 top-1/2 -translate-y-1/2 text-white/80 hover:text-white"
            onClick={e => { e.stopPropagation(); next(); }}>
            <ChevronRight size={36} />
          </button>
          <p className="absolute bottom-4 text-white/60 text-sm">{selectedImg + 1} / {orderedImages.length}</p>
        </div>
      )}
    </>
  );
}
