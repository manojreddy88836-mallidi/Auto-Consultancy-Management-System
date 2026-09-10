import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  CreditCard, Search, Filter, RefreshCw, Eye, TrendingUp,
  Calculator, IndianRupee, X, CheckCircle2, AlertTriangle, PlusCircle
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import { getAllApplicationsAdmin } from '../../api/applicationApi';
import { getEmiPayments, triggerOverdueRefresh } from '../../api/emiPaymentApi';
import { calculateEMI, fmtINR } from '../../utils/emiCalculator';
import EmiPaymentModal from './EmiPaymentModal';
import EmiDetailModal  from './EmiDetailModal';

// ─── Overdue status config ────────────────────────────────────────────────────
const overdueConfig = {
  ON_TIME:    { label: 'On Time',         badge: 'bg-green-100 text-green-700',   border: 'border-l-green-400',  icon: '🟢' },
  ONE_MONTH:  { label: '1 Month Overdue', badge: 'bg-yellow-100 text-yellow-700', border: 'border-l-yellow-400', icon: '🟡' },
  TWO_MONTHS: { label: '2 Months Overdue',badge: 'bg-orange-100 text-orange-700', border: 'border-l-orange-500', icon: '🟠' },
  CRITICAL:   { label: 'Critical Overdue',badge: 'bg-red-100 text-red-700',       border: 'border-l-red-500',    icon: '🔴' },
};

const getOverdueCfg = (status) => overdueConfig[status] || overdueConfig.ON_TIME;

// ─── Status badge styles (existing loan closure status) ──────────────────────
const loanStatusStyle = {
  ACTIVE:      'bg-blue-100 text-blue-700',
  COMPLETED:   'bg-green-100 text-green-700',
  CLOSED:      'bg-gray-100 text-gray-600',
  NOC_PENDING: 'bg-yellow-100 text-yellow-700',
  NO_FINANCE:  'bg-purple-100 text-purple-700',
};

// ─── Inline EMI Calculator Panel ─────────────────────────────────────────────
function EmiCalculatorPanel({ onClose }) {
  const [principal, setPrincipal] = useState('');
  const [rate,      setRate]      = useState('');
  const [months,    setMonths]    = useState('');

  const result = useMemo(() => {
    const m = Number(months);
    if (!principal || !months || m < 1) return null;
    return calculateEMI(Number(principal), rate === '' ? 0 : Number(rate), m);
  }, [principal, rate, months]);

  const inputCls = 'w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none';

  return (
    <div className="bg-gradient-to-br from-[#0F1B35] to-[#1E3A5F] text-white rounded-2xl p-6 shadow-xl">
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-2">
          <Calculator size={18} className="text-blue-300" />
          <h3 className="font-bold text-base">EMI Calculator</h3>
        </div>
        {onClose && (
          <button onClick={onClose} className="p-1 hover:bg-white/10 rounded-lg transition-colors">
            <X size={16} />
          </button>
        )}
      </div>

      <div className="space-y-3 mb-5">
        <div>
          <label className="block text-xs font-semibold text-blue-200 mb-1">Loan Amount (Rs.)</label>
          <div className="relative">
            <IndianRupee size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="number" value={principal} onChange={e => setPrincipal(e.target.value)}
              placeholder="e.g. 75000" className={inputCls + ' pl-8 bg-white text-gray-800'} min="1" />
          </div>
        </div>
        <div>
          <label className="block text-xs font-semibold text-blue-200 mb-1">Monthly Interest Rate (% per month)</label>
          <input type="number" value={rate} onChange={e => setRate(e.target.value)}
            placeholder="0 for no interest" className={inputCls + ' bg-white text-gray-800'} min="0" step="0.1" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-blue-200 mb-1">Tenure (Months)</label>
          <input type="number" value={months} onChange={e => {
            const v = parseInt(e.target.value, 10);
            setMonths(e.target.value === '' ? '' : String(Math.max(1, v || 1)));
          }}
            placeholder="12, 24, 36, 60..." className={inputCls + ' bg-white text-gray-800'} min="1" max="360" />
        </div>
      </div>

      {result?.valid && (
        <div className="bg-white/10 rounded-xl p-4 space-y-2">
          <div className="text-center">
            <p className="text-xs text-blue-200">Monthly EMI</p>
            <p className="text-2xl font-bold">{fmtINR(result.emi)}</p>
            <p className="text-xs text-blue-300 mt-0.5">for {result.totalMonths} months</p>
          </div>
          <div className="grid grid-cols-2 gap-2 pt-2">
            {[
              ['Total Payable', fmtINR(result.totalPayable)],
              ['Total Interest', fmtINR(result.totalInterest)],
            ].map(([l, v]) => (
              <div key={l} className="bg-white/10 rounded-lg p-2 text-center">
                <p className="text-[10px] text-blue-300">{l}</p>
                <p className="text-sm font-bold">{v}</p>
              </div>
            ))}
          </div>
        </div>
      )}
      {result && !result.valid && (
        <div className="bg-red-500/20 rounded-xl p-3 text-red-200 text-xs">{result.error}</div>
      )}
      {!result && (
        <div className="text-center text-blue-300 text-xs py-3">Enter amount and tenure to calculate</div>
      )}

      <div className="mt-4 pt-4 border-t border-white/10">
        <p className="text-[10px] text-blue-300 font-mono">
          Interest = P × (Rate/100) × Months | Total = P + Interest | EMI = Total ÷ Months
        </p>
        <p className="text-[10px] text-blue-400 mt-1">Full/Flat Interest — Rate is % per month — Tenure in Months</p>
      </div>
    </div>
  );
}

// ─── Main Finance Records Page ────────────────────────────────────────────────
const FinanceRecordsPage = () => {
  const navigate = useNavigate();
  const [records,        setRecords]        = useState([]);
  const [loading,        setLoading]        = useState(true);
  const [search,         setSearch]         = useState('');
  const [financeFilter,  setFinanceFilter]  = useState('');
  const [overdueFilter,  setOverdueFilter]  = useState('');
  const [page,           setPage]           = useState(0);
  const [totalPages,     setTotalPages]     = useState(1);
  const [totalElements,  setTotalElements]  = useState(0);
  const [showCalc,       setShowCalc]       = useState(false);
  const [refreshing,     setRefreshing]     = useState(false);

  // Modals
  const [payModal,       setPayModal]       = useState(null);  // record to pay
  const [detailModal,    setDetailModal]    = useState(null);  // record to view history
  const [payModalPaid,   setPayModalPaid]   = useState(new Set());

  const fetchRecords = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getAllApplicationsAdmin({ page, size: 30, sort: 'createdAt,desc' });
      const d = res.data?.data;
      let content = Array.isArray(d) ? d : (d?.content || []);
      // Only show apps that have finance data submitted
      content = content.filter(a => a.underFinance !== null && a.underFinance !== undefined);
      setRecords(content);
      setTotalPages(d?.totalPages || 1);
      setTotalElements(d?.totalElements || content.length);
    } catch { toast.error('Failed to load finance records'); }
    finally { setLoading(false); }
  }, [page]);

  useEffect(() => { fetchRecords(); }, [fetchRecords]);

  // Trigger overdue refresh on page load (in background)
  useEffect(() => {
    triggerOverdueRefresh().catch(() => {}); // silent — just keeps status fresh
  }, []);

  // ── Summary stats ──────────────────────────────────────────────────────────
  const financed    = records.filter(r => r.underFinance === true);
  const noFinance   = records.filter(r => r.underFinance === false);
  const oneMo       = financed.filter(r => r.overdueStatus === 'ONE_MONTH');
  const twoMo       = financed.filter(r => r.overdueStatus === 'TWO_MONTHS');
  const critical    = financed.filter(r => r.overdueStatus === 'CRITICAL');
  const totalOutstanding = financed.reduce((s, r) => s + Number(r.outstandingLoanAmount || 0), 0);

  // ── Filter & search ────────────────────────────────────────────────────────
  const filtered = useMemo(() => {
    let list = records;
    if (financeFilter === 'financed')   list = list.filter(r => r.underFinance === true);
    if (financeFilter === 'no-finance') list = list.filter(r => r.underFinance === false);
    if (overdueFilter) list = list.filter(r => r.overdueStatus === overdueFilter);

    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(r =>
        r.customerName?.toLowerCase().includes(q)
        || r.applicationNumber?.toLowerCase().includes(q)
        || r.financeCompany?.toLowerCase().includes(q)
      );
    }
    return list;
  }, [records, financeFilter, overdueFilter, search]);

  // ── Handle "Record Payment" button ────────────────────────────────────────
  const handleRecordPayment = async (record) => {
    setPayModal(record);
    if (record.financeDetailId) {
      try {
        const res = await getEmiPayments(record.financeDetailId);
        const payments = res.data?.data || [];
        setPayModalPaid(new Set(payments.map(p => p.installmentNumber)));
      } catch { setPayModalPaid(new Set()); }
    }
  };

  // ── Manual overdue refresh ─────────────────────────────────────────────────
  const handleManualRefresh = async () => {
    setRefreshing(true);
    try {
      await triggerOverdueRefresh();
      toast.success('Overdue status refreshed for all records');
      fetchRecords();
    } catch { toast.error('Refresh failed'); }
    finally { setRefreshing(false); }
  };

  return (
    <div className="space-y-6">

      {/* ── Header ────────────────────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <CreditCard className="text-[#1E88E5]" size={24}/> Finance Records
          </h1>
          <p className="text-gray-500 text-sm mt-1">{totalElements} applications with finance data</p>
        </div>
        <div className="flex items-center gap-2 self-start flex-wrap">
          {critical.length > 0 && (
            <button
              onClick={() => { setOverdueFilter('CRITICAL'); setFinanceFilter('financed'); }}
              className="flex items-center gap-1.5 px-3 py-2 bg-red-600 text-white rounded-xl text-xs font-bold shadow-sm animate-pulse hover:animate-none hover:bg-red-700 transition-colors"
            >
              🔴 {critical.length} Critical
            </button>
          )}
          {twoMo.length > 0 && (
            <button
              onClick={() => { setOverdueFilter('TWO_MONTHS'); setFinanceFilter('financed'); }}
              className="flex items-center gap-1.5 px-3 py-2 bg-orange-500 text-white rounded-xl text-xs font-bold shadow-sm hover:bg-orange-600 transition-colors"
            >
              🟠 {twoMo.length} Warning
            </button>
          )}
          <button
            onClick={() => setShowCalc(c => !c)}
            className={`flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-semibold transition-all shadow-sm ${
              showCalc ? 'bg-[#0F1B35] text-white' : 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-50'
            }`}
          >
            <Calculator size={15}/> EMI Calculator
          </button>
          <button
            onClick={handleManualRefresh}
            disabled={refreshing}
            title="Refresh overdue status"
            className="p-2 hover:bg-gray-100 rounded-lg disabled:opacity-50"
          >
            <RefreshCw size={18} className={`text-gray-500 ${refreshing ? 'animate-spin' : ''}`}/>
          </button>
        </div>
      </div>

      {/* ── Inline EMI Calculator ──────────────────────────────────────────── */}
      {showCalc && (
        <div className="max-w-sm">
          <EmiCalculatorPanel onClose={() => setShowCalc(false)} />
        </div>
      )}

      {/* ── Summary Cards ─────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3">
        {[
          { label: 'Total Records',     value: records.length,       color: 'from-blue-500 to-blue-600',       fmtFn: v => v,       filter: null },
          { label: 'Under Finance',     value: financed.length,      color: 'from-orange-400 to-orange-500',   fmtFn: v => v,       filter: { f: 'financed', o: '' } },
          { label: 'No Finance (Paid)', value: noFinance.length,     color: 'from-emerald-500 to-emerald-600', fmtFn: v => v,       filter: { f: 'no-finance', o: '' } },
          { label: 'Outstanding Total', value: totalOutstanding,     color: 'from-gray-500 to-gray-600',       fmtFn: fmtINR,       filter: null },
          { label: '🟡 1 Month',        value: oneMo.length,         color: 'from-yellow-400 to-yellow-500',   fmtFn: v => v,       filter: { f: 'financed', o: 'ONE_MONTH' } },
          { label: '🟠 2 Months',       value: twoMo.length,         color: 'from-orange-500 to-orange-600',   fmtFn: v => v,       filter: { f: 'financed', o: 'TWO_MONTHS' } },
          { label: '🔴 Critical',       value: critical.length,      color: 'from-red-500 to-red-600',         fmtFn: v => v,       filter: { f: 'financed', o: 'CRITICAL' } },
        ].map(s => (
          <div
            key={s.label}
            onClick={() => s.filter && (setFinanceFilter(s.filter.f), setOverdueFilter(s.filter.o), setPage(0))}
            className={`bg-gradient-to-br ${s.color} text-white rounded-2xl p-3 shadow-md ${s.filter ? 'cursor-pointer hover:opacity-90 transition-opacity' : ''}`}
          >
            <p className="text-xl font-bold">{s.fmtFn(s.value)}</p>
            <p className="text-[10px] font-medium text-white/80 mt-0.5 leading-tight">{s.label}</p>
          </div>
        ))}
      </div>

      {/* ── Filters ───────────────────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row gap-3 flex-wrap">
        <div className="relative flex-1 max-w-sm">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
          <input value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search customer, app#, company..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"/>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <Filter size={15} className="text-gray-400"/>
          <select value={financeFilter} onChange={e => { setFinanceFilter(e.target.value); setPage(0); }}
            className="px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
            <option value="">All Finance Types</option>
            <option value="financed">Under Finance</option>
            <option value="no-finance">No Finance (Paid)</option>
          </select>
          <select value={overdueFilter} onChange={e => { setOverdueFilter(e.target.value); setPage(0); }}
            className="px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white">
            <option value="">All Payment Status</option>
            <option value="ON_TIME">🟢 On Time</option>
            <option value="ONE_MONTH">🟡 1 Month Overdue</option>
            <option value="TWO_MONTHS">🟠 2 Months Overdue</option>
            <option value="CRITICAL">🔴 Critical Overdue</option>
          </select>
          {(financeFilter || overdueFilter || search) && (
            <button onClick={() => { setFinanceFilter(''); setOverdueFilter(''); setSearch(''); }}
              className="text-xs text-gray-500 hover:text-gray-700 underline">
              Clear filters
            </button>
          )}
        </div>
      </div>

      {/* ── Table ─────────────────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="w-8 h-8 border-3 border-[#1E88E5] border-t-transparent rounded-full animate-spin"/>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400 gap-3">
            <CreditCard size={40} className="opacity-25"/>
            <p className="text-sm font-medium">No finance records found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {[
                    'App #', 'Customer', 'Bike', 'Type',
                    'Loan Amount', 'Rate / Tenure', 'Monthly EMI',
                    'Missed EMIs', 'Outstanding', 'Payment Status',
                    'Paid / Left', 'Actions'
                  ].map(h => (
                    <th key={h} className="text-left px-4 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map(r => {
                  const fd = r;
                  const status = fd.overdueStatus || (fd.underFinance ? 'ON_TIME' : null);
                  const cfg = status ? getOverdueCfg(status) : null;
                  const missed = fd.missedEmiMonths || 0;

                  return (
                    <tr
                      key={r.id}
                      className={`hover:bg-gray-50/70 transition-colors border-l-4 ${
                        fd.underFinance && cfg ? cfg.border : 'border-l-transparent'
                      }`}
                    >
                      {/* App # */}
                      <td className="px-4 py-4 font-mono text-xs text-gray-500 whitespace-nowrap">
                        {r.applicationNumber}
                      </td>

                      {/* Customer */}
                      <td className="px-4 py-4">
                        <p className="font-semibold text-gray-900 text-xs">{r.customerName}</p>
                        <p className="text-xs text-gray-400">{r.customerPhone}</p>
                      </td>

                      {/* Bike */}
                      <td className="px-4 py-4 text-gray-600 text-xs whitespace-nowrap">
                        {r.manufacturerName} {r.modelName}
                      </td>

                      {/* Type */}
                      <td className="px-4 py-4">
                        <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${
                          r.underFinance ? 'bg-orange-100 text-orange-700' : 'bg-green-100 text-green-700'
                        }`}>
                          {r.underFinance ? '🏦 Financed' : '✅ Paid'}
                        </span>
                      </td>

                      {/* Loan Amount */}
                      <td className="px-4 py-4 text-gray-700 font-semibold text-xs whitespace-nowrap">
                        {fd.loanAmount ? fmtINR(fd.loanAmount) : '—'}
                      </td>

                      {/* Rate / Tenure */}
                      <td className="px-4 py-4 text-gray-500 text-xs whitespace-nowrap">
                        {fd.annualInterestRate != null ? `${fd.annualInterestRate}% p.m.` : '-'}
                        {(fd.tenureMonths || fd.tenureYears) && (
                          <span className="block text-gray-400">
                            {fd.tenureMonths ? `${fd.tenureMonths} mo` : `${fd.tenureYears * 12} mo`}
                          </span>
                        )}
                      </td>

                      {/* Monthly EMI */}
                      <td className="px-4 py-4 whitespace-nowrap">
                        {fd.emiAmount ? (
                          <span className="inline-block bg-[#1E88E5] text-white font-bold text-xs px-2.5 py-1 rounded-lg">
                            {fmtINR(fd.emiAmount)}/mo
                          </span>
                        ) : '—'}
                      </td>

                      {/* Missed EMIs — NEW */}
                      <td className="px-4 py-4 whitespace-nowrap">
                        {fd.underFinance ? (
                          missed > 0 ? (
                            <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold ${
                              missed >= 3 ? 'bg-red-100 text-red-700' :
                              missed === 2 ? 'bg-orange-100 text-orange-700' :
                              'bg-yellow-100 text-yellow-700'
                            }`}>
                              {missed >= 3 ? '🔴' : missed === 2 ? '🟠' : '🟡'} {missed} missed
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-green-100 text-green-700">
                              🟢 0 missed
                            </span>
                          )
                        ) : '—'}
                      </td>

                      {/* Outstanding — NEW */}
                      <td className="px-4 py-4 text-xs whitespace-nowrap">
                        {fd.underFinance && fd.outstandingLoanAmount != null ? (
                          <span className={`font-semibold ${missed > 0 ? 'text-red-600' : 'text-gray-700'}`}>
                            {fmtINR(fd.outstandingLoanAmount)}
                          </span>
                        ) : '—'}
                      </td>

                      {/* Payment Status — NEW */}
                      <td className="px-4 py-4 whitespace-nowrap">
                        {fd.underFinance && cfg ? (
                          <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold ${cfg.badge}`}>
                            {cfg.icon} {cfg.label}
                          </span>
                        ) : fd.underFinance ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-500">
                            — Unknown
                          </span>
                        ) : '—'}
                      </td>

                      {/* Paid / Remaining */}
                      <td className="px-4 py-4 text-xs whitespace-nowrap">
                        {fd.paidEmis != null && fd.numberOfEmis != null ? (
                          <div>
                            <span className="text-green-600 font-semibold">{fd.paidEmis} paid</span>
                            <span className="text-gray-400"> / </span>
                            <span className="text-red-500 font-semibold">{fd.remainingEmis ?? (fd.numberOfEmis - fd.paidEmis)} left</span>
                          </div>
                        ) : '—'}
                      </td>

                      {/* Actions */}
                      <td className="px-4 py-4">
                        <div className="flex items-center gap-1.5">
                          {fd.underFinance && fd.financeDetailId && (
                            <>
                              <button
                                onClick={() => handleRecordPayment(r)}
                                title="Record EMI payment"
                                className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold text-green-700 bg-green-50 hover:bg-green-100 rounded-lg transition-colors whitespace-nowrap"
                              >
                                <PlusCircle size={12}/> Pay
                              </button>
                              <button
                                onClick={() => setDetailModal(r)}
                                title="View payment history"
                                className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold text-blue-700 bg-blue-50 hover:bg-blue-100 rounded-lg transition-colors whitespace-nowrap"
                              >
                                <Eye size={12}/> History
                              </button>
                            </>
                          )}
                          <button
                            onClick={() => navigate(`/admin/applications/${r.id}`)}
                            className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-lg transition-colors whitespace-nowrap"
                          >
                            <Eye size={12}/> View
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-5 py-4 border-t border-gray-100 flex items-center justify-between">
            <p className="text-xs text-gray-500">Page {page + 1} of {totalPages}</p>
            <div className="flex gap-2">
              <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
                className="px-3 py-1.5 text-xs rounded-xl border border-gray-200 disabled:opacity-40 hover:bg-gray-50 font-medium">← Prev</button>
              <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
                className="px-3 py-1.5 text-xs rounded-xl border border-gray-200 disabled:opacity-40 hover:bg-gray-50 font-medium">Next →</button>
            </div>
          </div>
        )}
      </div>

      {/* ── EMI Formula Reference ──────────────────────────────────────────── */}
      <div className="bg-gray-50 border border-gray-200 rounded-xl px-5 py-4">
        <p className="text-xs font-semibold text-gray-500 mb-1 flex items-center gap-1">
          <TrendingUp size={12}/> EMI Formula (Full/Flat Interest)
        </p>
        <p className="text-xs text-gray-400 font-mono">
          Interest = P × (Rate/100) × Months &nbsp;|&nbsp;
          Total = P + Interest &nbsp;|&nbsp;
          EMI = Total ÷ Months &nbsp;<span className="text-blue-500 font-semibold">[Rate = % per month]</span>
        </p>
        <p className="text-xs text-gray-400 mt-1">
          Zero interest: EMI = Principal ÷ Months. &nbsp;
          <span className="text-blue-500 cursor-pointer hover:underline" onClick={() => setShowCalc(true)}>
            Open EMI Calculator →
          </span>
        </p>
      </div>

      {/* ── Modals ────────────────────────────────────────────────────────── */}
      {payModal && (
        <EmiPaymentModal
          record={payModal}
          paidSet={payModalPaid}
          onClose={() => { setPayModal(null); setPayModalPaid(new Set()); }}
          onSuccess={fetchRecords}
        />
      )}
      {detailModal && (
        <EmiDetailModal
          record={detailModal}
          onClose={() => setDetailModal(null)}
          onRefresh={fetchRecords}
        />
      )}
    </div>
  );
};

export default FinanceRecordsPage;
