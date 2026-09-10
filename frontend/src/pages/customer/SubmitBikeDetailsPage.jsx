import React, { useState, useMemo } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import {
  Check, ChevronRight, ChevronLeft, Upload, Loader, FileText,
  AlertCircle, Calculator, Bike, IndianRupee, Lock, ArrowLeft,
} from 'lucide-react';
import useAuth from '../../hooks/useAuth';
import * as applicationApi from '../../api/applicationApi';
import { uploadDocument } from '../../api/documentApi';
import { INDIAN_STATES, IDENTITY_PROOF_TYPES, PAYMENT_METHODS } from '../../utils/constants';
import { calculateEMI, fmtINR } from '../../utils/emiCalculator';

const BACKEND_BASE = 'http://localhost:8080';

// ── Steps ──────────────────────────────────────────────────────────────────
const STEPS = [
  { id: 1, title: 'Customer Info',  desc: 'Personal details'  },
  { id: 2, title: 'Applying For',   desc: 'Selected bike'     },
  { id: 3, title: 'Finance Info',   desc: 'Loan & payment'    },
  { id: 4, title: 'Documents',      desc: 'Upload files'      },
  { id: 5, title: 'Review',         desc: 'Confirm & submit'  },
];

const REQUIRED_DOCS = [
  { type: 'AADHAAR',          label: 'Aadhaar / Identity Proof', required: true  },
  { type: 'DRIVING_LICENCE',  label: 'Driving Licence',          required: true  },
  { type: 'PAN_CARD',         label: 'PAN Card',                 required: false },
  { type: 'ADDRESS_PROOF',    label: 'Address Proof',            required: false },
];

// ── Helpers ─────────────────────────────────────────────────────────────────
const inputClass  = 'w-full px-4 py-2.5 rounded-lg border border-gray-200 focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 outline-none transition-all text-sm bg-white';
const selectClass = `${inputClass} cursor-pointer`;

const FieldGroup = ({ label, required, children }) => (
  <div className="flex flex-col gap-1.5">
    <label className="text-sm font-semibold text-gray-700">
      {label} {required && <span className="text-red-500">*</span>}
    </label>
    {children}
  </div>
);

// Read-only info row used in "Applying For" step
const InfoRow = ({ label, value }) => (
  <div className="flex items-start justify-between py-2.5 border-b border-gray-100 last:border-0">
    <span className="text-sm text-gray-500 font-medium min-w-[130px]">{label}</span>
    <span className="text-sm font-semibold text-gray-800 text-right">{value || '—'}</span>
  </div>
);

// ── Main Component ───────────────────────────────────────────────────────────
export default function SubmitBikeDetailsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  const selectedBike    = location.state?.selectedBike    || null;
  const bikeInventoryId = location.state?.bikeInventoryId || null;

  const [step,          setStep]          = useState(1);
  const [submitting,    setSubmitting]    = useState(false);
  const [applicationId, setApplicationId] = useState(null);

  // ── Customer form state ──────────────────────────────────────────────────
  const [customer, setCustomer] = useState({
    fullName:            `${user?.firstName || ''} ${user?.lastName || ''}`.trim(),
    phone:               user?.phone  || '',
    email:               user?.email  || '',
    dob:                 '',
    address:             '',
    city:                '',
    state:               '',
    pincode:             '',
    identityProofType:   '',
    identityProofNumber: '',
  });

  // ── Finance form state ───────────────────────────────────────────────────
  const [finance, setFinance] = useState({
    underFinance:          null,
    loanAmount:            '',
    annualInterestRate:    '',
    tenureMonths:          '',
    financeCompany:        '',
    loanAccountNumber:     '',
    loanStartDate:         '',
    paidEmis:              '',
    outstandingLoanAmount: '',
    nextEmiDueDate:        '',
    emiPaymentStatus:      '',
    loanClosureStatus:     '',
    nocAvailable:          null,
    bikePaid:              null,
    purchasePaymentMethod: '',
  });

  // ── Documents state ──────────────────────────────────────────────────────
  const [documents, setDocuments] = useState({});
  const [errors,    setErrors]    = useState({});

  // ── EMI calculation ──────────────────────────────────────────────────────
  const emiResult = useMemo(() => {
    const m = Number(finance.tenureMonths);
    if (!finance.loanAmount || !finance.tenureMonths || m < 1) return null;
    const rate = finance.annualInterestRate === '' ? 0 : Number(finance.annualInterestRate);
    return calculateEMI(Number(finance.loanAmount), rate, m);
  }, [finance.loanAmount, finance.annualInterestRate, finance.tenureMonths]);

  const calcRemaining = (total, paid) => {
    const r = parseInt(total || 0) - parseInt(paid || 0);
    return r > 0 ? r : 0;
  };

  // ── Guard: must have a valid bike selected ───────────────────────────────
  if (!bikeInventoryId || !selectedBike) {
    return (
      <div className="max-w-lg mx-auto py-24 px-4 text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-50 mb-5">
          <AlertCircle size={32} className="text-red-400" />
        </div>
        <h2 className="text-xl font-bold text-gray-900 mb-2">No Bike Selected</h2>
        <p className="text-gray-500 text-sm mb-6">
          Please browse available bikes and click <strong>"Apply for This Bike"</strong> on the bike you want to apply for.
        </p>
        <button
          onClick={() => navigate('/customer/bikes')}
          className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-[#1E88E5] text-white font-semibold text-sm hover:bg-[#1976D2] transition-all shadow-md"
        >
          <ArrowLeft size={16} /> Browse Bikes
        </button>
      </div>
    );
  }

  // ── Bike info helpers ────────────────────────────────────────────────────
  const bikeImg   = selectedBike.primaryImageUrl ? `${BACKEND_BASE}${selectedBike.primaryImageUrl}` : null;
  const bikePrice = selectedBike.price != null ? fmtINR(selectedBike.price) : null;

  // ── Validation ───────────────────────────────────────────────────────────
  const validateStep = () => {
    const e = {};
    if (step === 1) {
      if (!customer.fullName.trim())  e.fullName = 'Name is required';
      if (!customer.phone.trim())     e.phone    = 'Phone is required';
      if (!customer.dob)              e.dob      = 'Date of birth is required';
      if (!customer.address.trim())   e.address  = 'Address is required';
      if (!customer.city.trim())      e.city     = 'City is required';
      if (!customer.state)            e.state    = 'State is required';
      if (!customer.pincode.trim())   e.pincode  = 'Pincode is required';
    }
    if (step === 3) {
      if (finance.underFinance === null) e.underFinance = 'Please answer whether the bike is under finance';
    }
    if (step === 4) {
      const missing = REQUIRED_DOCS.filter(d => d.required && !documents[d.type]);
      if (missing.length > 0)
        e.docs = `Please upload required documents: ${missing.map(d => d.label).join(', ')}`;
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleNext = () => { if (validateStep()) setStep(s => Math.min(s + 1, 5)); };
  const handleBack = () => setStep(s => Math.max(s - 1, 1));

  // ── File select (with 10 MB guard) ───────────────────────────────────────
  const handleFileSelect = (type, file) => {
    if (file.size > 10 * 1024 * 1024) { toast.error('File must be under 10 MB'); return; }
    setDocuments(d => ({ ...d, [type]: file }));
  };

  // ── Save draft ───────────────────────────────────────────────────────────
  const handleSaveDraft = async () => {
    setSubmitting(true);
    try {
      if (!applicationId) {
        const appRes = await applicationApi.createApplication(bikeInventoryId);
        const appId  = appRes.data?.data?.id;
        if (!appId) throw new Error('Failed to create application');
        setApplicationId(appId);
      }
      toast.success('Draft saved!');
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Could not save draft');
    } finally {
      setSubmitting(false);
    }
  };

  // ── Final submit ─────────────────────────────────────────────────────────
  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      // 1. Create application with bikeInventoryId — backend auto-fills BikeDetail
      const appRes = await applicationApi.createApplication(bikeInventoryId);
      const appId  = appRes.data?.data?.id;
      if (!appId) throw new Error('Failed to create application');
      setApplicationId(appId);

      // 2. Save finance details
      await applicationApi.updateFinanceDetails(appId, {
        underFinance:          finance.underFinance,
        loanAmount:            finance.loanAmount            ? parseFloat(finance.loanAmount)            : null,
        annualInterestRate:    finance.annualInterestRate !== '' ? parseFloat(finance.annualInterestRate) : 0,
        tenureMonths:          finance.tenureMonths           ? parseInt(finance.tenureMonths)              : null,
        financeCompany:        finance.financeCompany,
        loanAccountNumber:     finance.loanAccountNumber,
        loanStartDate:         finance.loanStartDate         || null,
        paidEmis:              finance.paidEmis              ? parseInt(finance.paidEmis)                : 0,
        outstandingLoanAmount: finance.outstandingLoanAmount ? parseFloat(finance.outstandingLoanAmount) : null,
        nextEmiDueDate:        finance.nextEmiDueDate        || null,
        emiPaymentStatus:      finance.emiPaymentStatus      || null,
        loanClosureStatus:     finance.loanClosureStatus     || null,
        nocAvailable:          finance.nocAvailable,
        bikePaid:              finance.bikePaid,
        purchasePaymentMethod: finance.purchasePaymentMethod,
      });

      // 3. Upload documents
      for (const [type, file] of Object.entries(documents)) {
        if (file) {
          const fd = new FormData();
          fd.append('file', file);
          fd.append('documentType', type);
          await uploadDocument(appId, fd).catch(() => {});
        }
      }

      // 4. Submit
      await applicationApi.submitApplication(appId);
      toast.success('Application submitted successfully!');
      navigate(`/customer/applications/${appId}`);
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Submission failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const fieldErr = (k) => errors[k] ? (
    <p className="text-red-500 text-xs mt-1 flex items-center gap-1"><AlertCircle size={12} /> {errors[k]}</p>
  ) : null;

  // ── Render ───────────────────────────────────────────────────────────────
  return (
    <div className="max-w-4xl mx-auto py-6 px-4">
      <div className="mb-8">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 mb-4 transition-colors"
        >
          <ArrowLeft size={15} /> Back
        </button>
        <h1 className="text-2xl font-bold text-gray-900">Bike Finance Application</h1>
        <p className="text-gray-500 text-sm mt-1">Complete all 5 steps to submit your application</p>
      </div>

      {/* ── Step Indicator ─────────────────────────────────────────────── */}
      <div className="mb-10 relative">
        <div className="absolute top-5 left-0 w-full h-0.5 bg-gray-200 z-0">
          <div
            className="h-full bg-[#1E88E5] transition-all duration-500"
            style={{ width: `${((step - 1) / (STEPS.length - 1)) * 100}%` }}
          />
        </div>
        <div className="relative z-10 flex justify-between">
          {STEPS.map((s) => (
            <div key={s.id} className="flex flex-col items-center gap-2">
              <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm border-2 transition-all duration-300 ${
                s.id < step  ? 'bg-[#1E88E5] border-[#1E88E5] text-white shadow-md shadow-blue-200'
                : s.id === step ? 'bg-white border-[#1E88E5] text-[#1E88E5] shadow-lg shadow-blue-100'
                : 'bg-white border-gray-200 text-gray-400'
              }`}>
                {s.id < step ? <Check size={16} /> : s.id}
              </div>
              <div className="text-center hidden sm:block">
                <p className={`text-xs font-semibold ${s.id <= step ? 'text-gray-800' : 'text-gray-400'}`}>{s.title}</p>
                <p className={`text-xs ${s.id <= step ? 'text-gray-500' : 'text-gray-300'}`}>{s.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Main Card ──────────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-6 sm:px-8 py-8">

          {/* ═══════════════════════════════════════════════════════
              STEP 1 — CUSTOMER INFO
          ════════════════════════════════════════════════════════ */}
          {step === 1 && (
            <div className="space-y-6">
              <h2 className="text-lg font-bold text-gray-900 border-b border-gray-100 pb-3">
                Personal Information
              </h2>
              <div className="grid md:grid-cols-2 gap-5">
                <FieldGroup label="Full Name" required>
                  <input className={inputClass} value={customer.fullName}
                    onChange={e => setCustomer({ ...customer, fullName: e.target.value })}
                    placeholder="John Doe" />
                  {fieldErr('fullName')}
                </FieldGroup>
                <FieldGroup label="Mobile Number" required>
                  <input className={inputClass} value={customer.phone} maxLength={10}
                    onChange={e => setCustomer({ ...customer, phone: e.target.value.replace(/\D/, '') })}
                    placeholder="9876543210" />
                  {fieldErr('phone')}
                </FieldGroup>
                <FieldGroup label="Email Address">
                  <input className={`${inputClass} bg-gray-50`} value={customer.email} readOnly />
                </FieldGroup>
                <FieldGroup label="Date of Birth" required>
                  <input type="date" className={inputClass} value={customer.dob}
                    max={new Date(new Date().setFullYear(new Date().getFullYear() - 18)).toISOString().split('T')[0]}
                    onChange={e => setCustomer({ ...customer, dob: e.target.value })} />
                  {fieldErr('dob')}
                </FieldGroup>
                <div className="md:col-span-2">
                  <FieldGroup label="Full Address" required>
                    <input className={inputClass} value={customer.address}
                      onChange={e => setCustomer({ ...customer, address: e.target.value })}
                      placeholder="House No., Street, Area" />
                    {fieldErr('address')}
                  </FieldGroup>
                </div>
                <FieldGroup label="City" required>
                  <input className={inputClass} value={customer.city}
                    onChange={e => setCustomer({ ...customer, city: e.target.value })}
                    placeholder="Mumbai" />
                  {fieldErr('city')}
                </FieldGroup>
                <FieldGroup label="State" required>
                  <select className={selectClass} value={customer.state}
                    onChange={e => setCustomer({ ...customer, state: e.target.value })}>
                    <option value="">Select State</option>
                    {INDIAN_STATES.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                  {fieldErr('state')}
                </FieldGroup>
                <FieldGroup label="Pincode" required>
                  <input className={inputClass} value={customer.pincode} maxLength={6}
                    onChange={e => setCustomer({ ...customer, pincode: e.target.value.replace(/\D/, '') })}
                    placeholder="400001" />
                  {fieldErr('pincode')}
                </FieldGroup>
                <FieldGroup label="Identity Proof Type">
                  <select className={selectClass} value={customer.identityProofType}
                    onChange={e => setCustomer({ ...customer, identityProofType: e.target.value })}>
                    <option value="">Select Type</option>
                    {IDENTITY_PROOF_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </FieldGroup>
                <div className="md:col-span-2">
                  <FieldGroup label="Identity Proof Number">
                    <input className={inputClass} value={customer.identityProofNumber}
                      onChange={e => setCustomer({ ...customer, identityProofNumber: e.target.value })}
                      placeholder="Aadhaar / PAN / DL number" />
                  </FieldGroup>
                </div>
              </div>
            </div>
          )}

          {/* ═══════════════════════════════════════════════════════
              STEP 2 — APPLYING FOR (read-only bike display)
          ════════════════════════════════════════════════════════ */}
          {step === 2 && (
            <div className="space-y-6">
              {/* Header */}
              <div className="border-b border-gray-100 pb-3">
                <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <Bike size={20} className="text-[#1E88E5]" />
                  You Are Applying For
                </h2>
                <p className="text-sm text-gray-500 mt-1">
                  The bike details below are fixed and cannot be changed. They come from the inventory record.
                </p>
              </div>

              {/* Lock badge */}
              <div className="flex items-center gap-2 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3">
                <Lock size={15} className="text-amber-600 shrink-0" />
                <p className="text-sm text-amber-700 font-medium">
                  These details are locked to the selected inventory item and cannot be edited.
                </p>
              </div>

              {/* Bike card */}
              <div className="bg-gray-50 rounded-2xl border border-gray-200 overflow-hidden">
                {/* Image */}
                <div className="relative h-56 bg-gradient-to-br from-slate-100 to-slate-200">
                  {bikeImg ? (
                    <img src={bikeImg} alt={selectedBike.modelName} className="w-full h-full object-cover" />
                  ) : (
                    <div className="absolute inset-0 flex flex-col items-center justify-center text-gray-400">
                      <Bike size={52} className="opacity-20 mb-2" />
                      <span className="text-sm opacity-40">No image available</span>
                    </div>
                  )}
                  <div className="absolute top-3 left-3">
                    <span className="bg-emerald-500 text-white text-xs font-bold px-3 py-1 rounded-full shadow">
                      AVAILABLE
                    </span>
                  </div>
                  {selectedBike.conditionType && (
                    <div className="absolute top-3 right-3">
                      <span className="bg-black/50 text-white text-xs px-2.5 py-1 rounded-full backdrop-blur-sm">
                        {selectedBike.conditionType}
                      </span>
                    </div>
                  )}
                </div>

                {/* Bike info rows */}
                <div className="p-5 space-y-0">
                  <div className="mb-3">
                    <p className="text-xs font-semibold text-[#1E88E5] uppercase tracking-wide">
                      {selectedBike.manufacturerName}
                    </p>
                    <h3 className="text-xl font-bold text-gray-900">
                      {selectedBike.modelName}
                    </h3>
                  </div>

                  {bikePrice && (
                    <div className="mb-4 flex items-center gap-1.5 bg-blue-50 border border-blue-100 rounded-xl px-4 py-3">
                      <IndianRupee size={16} className="text-blue-700" />
                      <span className="text-blue-700 font-bold text-lg">{bikePrice}</span>
                      <span className="text-blue-500 text-xs ml-1">Listed Price</span>
                    </div>
                  )}

                  <InfoRow label="Brand"              value={selectedBike.manufacturerName} />
                  <InfoRow label="Model"              value={selectedBike.modelName} />
                  {selectedBike.variantName  && <InfoRow label="Variant"    value={selectedBike.variantName} />}
                  {selectedBike.manufacturingYear && <InfoRow label="Manufacturing Year" value={selectedBike.manufacturingYear} />}
                  {selectedBike.registrationNumber && <InfoRow label="Registration No." value={selectedBike.registrationNumber} />}
                  {selectedBike.colour       && <InfoRow label="Colour"     value={selectedBike.colour} />}
                  {selectedBike.fuelType     && <InfoRow label="Fuel Type"  value={selectedBike.fuelType} />}
                  {selectedBike.kilometersDriven != null && (
                    <InfoRow label="Kilometres Driven" value={`${selectedBike.kilometersDriven.toLocaleString()} km`} />
                  )}
                  {selectedBike.bikeCode     && <InfoRow label="Bike Code"  value={selectedBike.bikeCode} />}
                  <InfoRow label="Inventory ID" value={`#${bikeInventoryId}`} />
                </div>
              </div>
            </div>
          )}

          {/* ═══════════════════════════════════════════════════════
              STEP 3 — FINANCE INFO
          ════════════════════════════════════════════════════════ */}
          {step === 3 && (
            <div className="space-y-6">
              <h2 className="text-lg font-bold text-gray-900 border-b border-gray-100 pb-3">Finance Information</h2>

              <div className="space-y-3">
                <label className="block text-sm font-semibold text-gray-700">
                  Is this bike currently under finance / loan? <span className="text-red-500">*</span>
                </label>
                <div className="flex gap-4">
                  {[{ val: true, label: '💳 Yes, it is under finance' }, { val: false, label: '✅ No, fully paid' }].map(opt => (
                    <label key={String(opt.val)} className={`flex-1 flex items-center gap-3 p-4 rounded-xl border-2 cursor-pointer transition-all ${
                      finance.underFinance === opt.val ? 'border-[#1E88E5] bg-blue-50' : 'border-gray-200 hover:border-gray-300'
                    }`}>
                      <input type="radio" checked={finance.underFinance === opt.val}
                        onChange={() => setFinance({ ...finance, underFinance: opt.val })}
                        className="w-4 h-4 accent-[#1E88E5]" />
                      <span className="text-sm font-medium text-gray-700">{opt.label}</span>
                    </label>
                  ))}
                </div>
                {fieldErr('underFinance')}
              </div>

              {finance.underFinance === true && (
                <div className="space-y-5 border-t border-gray-100 pt-5">
                  <p className="text-sm font-semibold text-blue-700">📋 Loan Details</p>

                  {/* EMI calculator */}
                  <div className="bg-blue-50 border border-blue-100 rounded-xl p-5 space-y-4">
                    <div className="flex items-center gap-2 mb-1">
                      <Calculator size={16} className="text-blue-600" />
                      <span className="text-sm font-semibold text-blue-800">EMI Calculator</span>
                    </div>
                    <div className="grid md:grid-cols-3 gap-4">
                      <FieldGroup label="Loan Amount (₹)">
                        <input className={inputClass} type="number" value={finance.loanAmount}
                          onChange={e => setFinance({ ...finance, loanAmount: e.target.value })}
                          placeholder="500000" min={0} />
                      </FieldGroup>
                      <FieldGroup label="Interest Rate (% p.a.)">
                        <input className={inputClass} type="number" value={finance.annualInterestRate}
                          onChange={e => setFinance({ ...finance, annualInterestRate: e.target.value })}
                          placeholder="9.5" step="0.01" min={0} />
                      </FieldGroup>
                      <FieldGroup label="Tenure (Months)">
                        <input className={inputClass} type="number" value={finance.tenureMonths}
                          onChange={e => {
                            const v = parseInt(e.target.value, 10);
                            setFinance({ ...finance, tenureMonths: e.target.value === '' ? '' : String(Math.max(1, v || 1)) });
                          }}
                          placeholder="e.g. 12, 24, 36, 60" min={1} max={360} />
                      </FieldGroup>
                    </div>
                    {emiResult && (
                      <div className="grid grid-cols-3 gap-3 pt-2 border-t border-blue-200">
                        {[
                          { label: 'Monthly EMI',   value: fmtINR(emiResult.emi) },
                          { label: 'Total Payable', value: fmtINR(emiResult.totalPayable) },
                          { label: 'Total Interest',value: fmtINR(emiResult.totalInterest) },
                        ].map(item => (
                          <div key={item.label} className="bg-white rounded-lg p-3 text-center shadow-sm">
                            <p className="text-xs text-gray-500 mb-1">{item.label}</p>
                            <p className="text-sm font-bold text-gray-900">{item.value}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="grid md:grid-cols-2 gap-5">
                    <FieldGroup label="Finance Company">
                      <input className={inputClass} value={finance.financeCompany}
                        onChange={e => setFinance({ ...finance, financeCompany: e.target.value })}
                        placeholder="HDFC / Bajaj / Hero Fincorp..." />
                    </FieldGroup>
                    <FieldGroup label="Loan Account Number">
                      <input className={inputClass} value={finance.loanAccountNumber}
                        onChange={e => setFinance({ ...finance, loanAccountNumber: e.target.value })}
                        placeholder="Loan account / reference" />
                    </FieldGroup>
                    <FieldGroup label="Loan Start Date">
                      <input type="date" className={inputClass} value={finance.loanStartDate}
                        onChange={e => setFinance({ ...finance, loanStartDate: e.target.value })} />
                    </FieldGroup>
                    <FieldGroup label="EMIs Paid">
                      <input className={inputClass} type="number" value={finance.paidEmis}
                        onChange={e => setFinance({ ...finance, paidEmis: e.target.value })}
                        placeholder="0" min={0} />
                    </FieldGroup>
                    <FieldGroup label="Outstanding Loan Amount (₹)">
                      <input className={inputClass} type="number" value={finance.outstandingLoanAmount}
                        onChange={e => setFinance({ ...finance, outstandingLoanAmount: e.target.value })}
                        placeholder="200000" min={0} />
                    </FieldGroup>
                    <FieldGroup label="Next EMI Due Date">
                      <input type="date" className={inputClass} value={finance.nextEmiDueDate}
                        onChange={e => setFinance({ ...finance, nextEmiDueDate: e.target.value })} />
                    </FieldGroup>
                    <FieldGroup label="Loan Closure Status">
                      <select className={selectClass} value={finance.loanClosureStatus}
                        onChange={e => setFinance({ ...finance, loanClosureStatus: e.target.value })}>
                        <option value="">Select Status</option>
                        {['ACTIVE', 'COMPLETED', 'CLOSED'].map(s => <option key={s} value={s}>{s}</option>)}
                      </select>
                    </FieldGroup>
                    <FieldGroup label="NOC Available?">
                      <div className="flex gap-3 mt-1">
                        {[{ val: true, label: 'Yes' }, { val: false, label: 'No' }].map(opt => (
                          <label key={String(opt.val)} className={`flex items-center gap-2 px-4 py-2 rounded-lg border cursor-pointer text-sm transition-all ${
                            finance.nocAvailable === opt.val ? 'border-[#1E88E5] bg-blue-50 text-blue-700 font-medium' : 'border-gray-200 text-gray-600'
                          }`}>
                            <input type="radio" checked={finance.nocAvailable === opt.val}
                              onChange={() => setFinance({ ...finance, nocAvailable: opt.val })}
                              className="w-4 h-4 accent-[#1E88E5]" />
                            {opt.label}
                          </label>
                        ))}
                      </div>
                    </FieldGroup>
                  </div>
                </div>
              )}

              {finance.underFinance === false && (
                <div className="space-y-5 border-t border-gray-100 pt-5">
                  <p className="text-sm font-semibold text-green-700">✅ No Finance</p>
                  <div className="grid md:grid-cols-2 gap-5">
                    <FieldGroup label="Was full amount paid?">
                      <div className="flex gap-3 mt-1">
                        {[{ val: true, label: 'Yes' }, { val: false, label: 'No' }].map(opt => (
                          <label key={String(opt.val)} className={`flex items-center gap-2 px-4 py-2 rounded-lg border cursor-pointer text-sm transition-all ${
                            finance.bikePaid === opt.val ? 'border-[#1E88E5] bg-blue-50 text-blue-700 font-medium' : 'border-gray-200 text-gray-600'
                          }`}>
                            <input type="radio" checked={finance.bikePaid === opt.val}
                              onChange={() => setFinance({ ...finance, bikePaid: opt.val })}
                              className="w-4 h-4 accent-[#1E88E5]" />
                            {opt.label}
                          </label>
                        ))}
                      </div>
                    </FieldGroup>
                    <FieldGroup label="Purchase Payment Method">
                      <select className={selectClass} value={finance.purchasePaymentMethod}
                        onChange={e => setFinance({ ...finance, purchasePaymentMethod: e.target.value })}>
                        <option value="">Select Method</option>
                        {PAYMENT_METHODS.map(m => <option key={m} value={m}>{m}</option>)}
                      </select>
                    </FieldGroup>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ═══════════════════════════════════════════════════════
              STEP 4 — DOCUMENTS
          ════════════════════════════════════════════════════════ */}
          {step === 4 && (
            <div className="space-y-6">
              <div className="border-b border-gray-100 pb-3">
                <h2 className="text-lg font-bold text-gray-900">Document Upload</h2>
                <p className="text-sm text-gray-500 mt-1">Upload clear, readable copies. Max 10 MB per file. PDF / JPG / PNG accepted.</p>
              </div>
              {errors.docs && (
                <div className="bg-red-50 border-l-4 border-red-500 p-3 rounded-r-lg">
                  <p className="text-red-700 text-sm">{errors.docs}</p>
                </div>
              )}
              <div className="grid md:grid-cols-2 gap-5">
                {REQUIRED_DOCS.map(doc => (
                  <div key={doc.type} className={`border-2 rounded-xl p-4 transition-all ${
                    documents[doc.type] ? 'border-green-300 bg-green-50' : 'border-dashed border-gray-200 hover:border-blue-300'
                  }`}>
                    <div className="flex items-start justify-between mb-3">
                      <div>
                        <p className="text-sm font-semibold text-gray-800">{doc.label}</p>
                        {doc.required && <span className="text-xs text-red-500 font-medium">* Required</span>}
                      </div>
                      {documents[doc.type] && (
                        <span className="flex items-center gap-1 text-xs text-green-600 font-medium">
                          <Check size={14} /> Uploaded
                        </span>
                      )}
                    </div>
                    {documents[doc.type] ? (
                      <div className="flex items-center justify-between bg-white rounded-lg px-3 py-2 border border-green-200">
                        <div className="flex items-center gap-2">
                          <FileText size={14} className="text-green-600" />
                          <span className="text-xs text-gray-700 truncate max-w-[150px]">{documents[doc.type].name}</span>
                        </div>
                        <button onClick={() => setDocuments(d => { const n = { ...d }; delete n[doc.type]; return n; })}
                          className="text-xs text-red-500 hover:text-red-700 font-medium ml-2">Remove</button>
                      </div>
                    ) : (
                      <label className="flex flex-col items-center gap-2 cursor-pointer py-3">
                        <Upload size={20} className="text-gray-400" />
                        <span className="text-xs text-gray-500">Click to upload</span>
                        <input type="file" accept=".pdf,.jpg,.jpeg,.png,.webp" className="hidden"
                          onChange={e => { if (e.target.files[0]) handleFileSelect(doc.type, e.target.files[0]); }} />
                      </label>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* ═══════════════════════════════════════════════════════
              STEP 5 — REVIEW & SUBMIT
          ════════════════════════════════════════════════════════ */}
          {step === 5 && (
            <div className="space-y-6">
              <h2 className="text-lg font-bold text-gray-900 border-b border-gray-100 pb-3">Review Your Application</h2>
              <p className="text-sm text-gray-500">Please verify all details before submitting. Once submitted, changes require contacting admin.</p>

              <div className="grid md:grid-cols-2 gap-4">
                {/* Customer summary */}
                <div className="bg-gray-50 rounded-xl p-5 border border-gray-100">
                  <h3 className="font-bold text-gray-800 mb-3 text-sm flex items-center gap-2">👤 Customer Details</h3>
                  <div className="space-y-1.5 text-sm">
                    <p><span className="text-gray-500">Name:</span> <span className="font-medium">{customer.fullName || '—'}</span></p>
                    <p><span className="text-gray-500">Phone:</span> <span className="font-medium">{customer.phone || '—'}</span></p>
                    <p><span className="text-gray-500">DOB:</span> <span className="font-medium">{customer.dob || '—'}</span></p>
                    <p><span className="text-gray-500">Address:</span> <span className="font-medium">{customer.address || '—'}</span></p>
                    <p><span className="text-gray-500">City, State:</span> <span className="font-medium">{customer.city}{customer.state ? `, ${customer.state}` : ''}</span></p>
                    <p><span className="text-gray-500">Pincode:</span> <span className="font-medium">{customer.pincode || '—'}</span></p>
                    <p><span className="text-gray-500">ID Proof:</span> <span className="font-medium">{customer.identityProofType || '—'}</span></p>
                  </div>
                </div>

                {/* Bike summary — read-only from inventory */}
                <div className="bg-blue-50 rounded-xl p-5 border border-blue-100">
                  <h3 className="font-bold text-gray-800 mb-3 text-sm flex items-center gap-2">🏍️ Bike Being Applied For</h3>
                  {bikeImg && (
                    <img src={bikeImg} alt={selectedBike.modelName} className="w-full h-32 object-cover rounded-xl mb-3" />
                  )}
                  <div className="space-y-1.5 text-sm">
                    <p><span className="text-gray-500">Brand:</span> <span className="font-medium">{selectedBike.manufacturerName || '—'}</span></p>
                    <p><span className="text-gray-500">Model:</span> <span className="font-medium">{selectedBike.modelName || '—'}</span></p>
                    {selectedBike.variantName && <p><span className="text-gray-500">Variant:</span> <span className="font-medium">{selectedBike.variantName}</span></p>}
                    {selectedBike.manufacturingYear && <p><span className="text-gray-500">Year:</span> <span className="font-medium">{selectedBike.manufacturingYear}</span></p>}
                    {selectedBike.registrationNumber && <p><span className="text-gray-500">Reg No.:</span> <span className="font-medium">{selectedBike.registrationNumber}</span></p>}
                    {selectedBike.colour && <p><span className="text-gray-500">Colour:</span> <span className="font-medium">{selectedBike.colour}</span></p>}
                    {bikePrice && <p><span className="text-gray-500">Price:</span> <span className="font-bold text-blue-700">{bikePrice}</span></p>}
                    <p><span className="text-gray-500">Inventory ID:</span> <span className="font-medium text-blue-600">#{bikeInventoryId}</span></p>
                  </div>
                  <div className="mt-3 flex items-center gap-1.5 text-xs text-gray-400">
                    <Lock size={11} /> Bike details are locked from the inventory record
                  </div>
                </div>

                {/* Finance summary */}
                <div className="bg-gray-50 rounded-xl p-5 border border-gray-100">
                  <h3 className="font-bold text-gray-800 mb-3 text-sm flex items-center gap-2">💰 Finance Details</h3>
                  <div className="space-y-1.5 text-sm">
                    <p><span className="text-gray-500">Under Finance:</span>{' '}
                      <span className={`font-medium ${finance.underFinance ? 'text-orange-600' : 'text-green-600'}`}>
                        {finance.underFinance === null ? '—' : finance.underFinance ? 'Yes' : 'No'}
                      </span>
                    </p>
                    {finance.underFinance && (<>
                      <p><span className="text-gray-500">Company:</span> <span className="font-medium">{finance.financeCompany || '—'}</span></p>
                      <p><span className="text-gray-500">Loan Amount:</span> <span className="font-medium">₹{finance.loanAmount || '—'}</span></p>
                      {emiResult && <p><span className="text-gray-500">Monthly EMI:</span> <span className="font-medium">{fmtINR(emiResult.emi)}/month</span></p>}
                      <p><span className="text-gray-500">Remaining EMIs:</span> <span className="font-medium">{calcRemaining(emiResult?.totalMonths, finance.paidEmis)}</span></p>
                    </>)}
                    {finance.underFinance === false && (
                      <p><span className="text-gray-500">Payment:</span> <span className="font-medium">{finance.purchasePaymentMethod || '—'}</span></p>
                    )}
                  </div>
                </div>

                {/* Documents summary */}
                <div className="bg-gray-50 rounded-xl p-5 border border-gray-100">
                  <h3 className="font-bold text-gray-800 mb-3 text-sm flex items-center gap-2">📄 Documents</h3>
                  <div className="space-y-1.5 text-sm">
                    {REQUIRED_DOCS.map(doc => (
                      <div key={doc.type} className="flex items-center justify-between">
                        <span className="text-gray-500">{doc.label}:</span>
                        <span className={`font-medium ${documents[doc.type] ? 'text-green-600' : doc.required ? 'text-red-500' : 'text-gray-400'}`}>
                          {documents[doc.type] ? '✓ Uploaded' : doc.required ? '✗ Missing' : 'Skipped'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* ── Navigation Footer ─────────────────────────────────── */}
        <div className="px-6 sm:px-8 py-5 bg-gray-50 border-t border-gray-100 flex justify-between items-center">
          <button type="button" onClick={handleBack} disabled={step === 1}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl border border-gray-200 text-gray-600 font-medium text-sm hover:bg-white transition-all disabled:opacity-40 disabled:cursor-not-allowed">
            <ChevronLeft size={16} /> Back
          </button>

          <div className="flex items-center gap-3">
            {step === 5 && (
              <button type="button" onClick={handleSaveDraft} disabled={submitting}
                className="px-5 py-2.5 rounded-xl border border-gray-300 text-gray-700 font-medium text-sm hover:bg-white transition-all disabled:opacity-60">
                Save as Draft
              </button>
            )}
            {step < 5 ? (
              <button type="button" onClick={handleNext}
                className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white font-semibold text-sm transition-all shadow-md hover:shadow-lg hover:scale-[1.02]">
                Next Step <ChevronRight size={16} />
              </button>
            ) : (
              <button type="button" onClick={handleSubmit} disabled={submitting}
                className="flex items-center gap-2 px-7 py-2.5 rounded-xl bg-[#0F1B35] hover:bg-[#1E3A5F] text-white font-bold text-sm transition-all shadow-lg disabled:opacity-60">
                {submitting
                  ? <><Loader size={16} className="animate-spin" /> Submitting...</>
                  : <><Check size={16} /> Submit Application</>}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
