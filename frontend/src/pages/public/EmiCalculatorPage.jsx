import React, { useState, useMemo } from 'react';
import { Calculator, IndianRupee, Percent, Calendar, TrendingUp, RefreshCw } from 'lucide-react';
import { calculateEMI, fmtINR } from '../../utils/emiCalculator';

// Examples now use months directly
const TEST_CASES = [
  { label: 'Example 1', principal: 15000,   rate: 2,  months: 12, desc: 'Rs.15K @ 2% x 12 mo' },
  { label: 'Example 2', principal: 100000,  rate: 10, months: 60, desc: 'Rs.1L @ 10% x 60 mo'  },
  { label: 'Example 3', principal: 50000,   rate: 0,  months: 24, desc: 'Rs.50K @ 0% x 24 mo'  },
  { label: 'Example 4', principal: 200000,  rate: 12, months: 36, desc: 'Rs.2L @ 12% x 36 mo'  },
];

export default function EmiCalculatorPage() {
  const [principal, setPrincipal] = useState('');
  const [rate,      setRate]      = useState('');
  const [months,    setMonths]    = useState('');

  const result = useMemo(() => {
    const m = Number(months);
    if (!principal || !months || m < 1) return null;
    return calculateEMI(
      Number(principal),
      rate === '' ? 0 : Number(rate),
      m
    );
  }, [principal, rate, months]);

  const loadExample = (ex) => {
    setPrincipal(String(ex.principal));
    setRate(String(ex.rate));
    setMonths(String(ex.months));
  };

  const reset = () => { setPrincipal(''); setRate(''); setMonths(''); };

  const inputCls = 'w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-500/20 focus:border-[#1E88E5] outline-none transition-all text-sm bg-white';

  return (
    <div className="min-h-screen bg-gray-50 pt-20">

      {/* Hero */}
      <div className="bg-[#0F1B35] py-16">
        <div className="max-w-3xl mx-auto px-4 text-center">
          <div className="inline-flex items-center gap-2 bg-blue-500/20 border border-blue-400/30 text-blue-200 text-sm font-semibold px-4 py-1.5 rounded-full mb-5">
            <Calculator size={15}/> Loan EMI Calculator
          </div>
          <h1 className="text-4xl md:text-5xl font-bold text-white mb-4">
            Calculate Your Bike EMI
          </h1>
          <p className="text-blue-100 text-lg max-w-xl mx-auto">
            Instantly calculate your monthly EMI using the Full/Flat Interest formula.
            Enter loan amount, interest rate, and tenure in months.
          </p>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-4 py-12">
        <div className="grid lg:grid-cols-5 gap-8">

          {/* Left: Calculator */}
          <div className="lg:col-span-3 space-y-6">
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <Calculator size={18} className="text-[#1E88E5]"/> Loan Details
                </h2>
                <button onClick={reset} className="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600 transition-colors">
                  <RefreshCw size={12}/> Reset
                </button>
              </div>

              {/* Inputs */}
              <div className="space-y-5">
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                    Loan Amount (Rs.) <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <IndianRupee size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
                    <input
                      type="number" value={principal} min="1"
                      onChange={e => setPrincipal(e.target.value)}
                      placeholder="e.g. 75000"
                      className={inputCls + ' pl-9'}
                      id="emi-principal"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                    Annual Interest Rate (%)
                    <span className="text-xs font-normal text-gray-400 ml-2">Leave 0 for no interest</span>
                  </label>
                  <div className="relative">
                    <Percent size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
                    <input
                      type="number" value={rate} min="0" step="0.1"
                      onChange={e => setRate(e.target.value)}
                      placeholder="e.g. 12"
                      className={inputCls + ' pl-9'}
                      id="emi-rate"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                    Tenure (Months) <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <Calendar size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
                    <input
                      type="number" value={months} min="1" max="360"
                      onChange={e => {
                        const v = parseInt(e.target.value, 10);
                        setMonths(e.target.value === '' ? '' : String(Math.max(1, v || 1)));
                      }}
                      placeholder="e.g. 6, 12, 24, 36, 60"
                      className={inputCls + ' pl-9'}
                      id="emi-months"
                    />
                  </div>
                </div>
              </div>

              {/* Results */}
              {result?.valid && (
                <div className="mt-6 space-y-3">
                  {/* Primary EMI */}
                  <div className="bg-gradient-to-r from-[#1E88E5] to-[#1565C0] rounded-xl p-5 text-white text-center">
                    <p className="text-sm font-semibold text-blue-100 mb-1">Monthly Payment (EMI)</p>
                    <p className="text-4xl font-bold" id="emi-result">{fmtINR(result.emi)}</p>
                    <p className="text-xs text-blue-200 mt-2">for {result.totalMonths} months</p>
                  </div>

                  {/* Breakdown */}
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { label: 'Total Payable',  value: fmtINR(result.totalPayable),              id: 'emi-total-payable',  cls: 'text-gray-800' },
                      { label: 'Total Interest', value: fmtINR(result.totalInterest),              id: 'emi-total-interest', cls: 'text-orange-600' },
                      { label: 'Principal',      value: fmtINR(Number(principal)),                 id: 'emi-principal-disp', cls: 'text-blue-700' },
                      { label: 'Annual Rate',    value: `${rate || 0}% p.a.`,                      id: 'emi-annual-rate',    cls: 'text-gray-600' },
                    ].map(item => (
                      <div key={item.label} className="bg-gray-50 rounded-xl p-3 border border-gray-100 text-center">
                        <p className="text-xs text-gray-500 font-semibold mb-1">{item.label}</p>
                        <p id={item.id} className={`text-base font-bold ${item.cls}`}>{item.value}</p>
                      </div>
                    ))}
                  </div>

                  {/* Interest proportion bar */}
                  <div>
                    <div className="flex justify-between text-xs text-gray-500 mb-1">
                      <span>Principal {((Number(principal) / result.totalPayable) * 100).toFixed(1)}%</span>
                      <span>Interest {((result.totalInterest / result.totalPayable) * 100).toFixed(1)}%</span>
                    </div>
                    <div className="h-2 bg-gray-100 rounded-full overflow-hidden flex">
                      <div
                        className="bg-[#1E88E5] h-full rounded-l-full transition-all"
                        style={{ width: `${(Number(principal) / result.totalPayable) * 100}%` }}
                      />
                      <div className="bg-orange-400 h-full rounded-r-full flex-1" />
                    </div>
                  </div>
                </div>
              )}

              {result && !result.valid && (
                <div className="mt-4 bg-red-50 border border-red-200 rounded-xl p-3 text-red-600 text-sm">
                  {result.error}
                </div>
              )}

              {!result && (
                <div className="mt-6 text-center text-gray-400 text-sm py-4">
                  <Calculator size={32} className="mx-auto mb-2 opacity-30"/>
                  <p>Enter loan amount and tenure to see your EMI</p>
                </div>
              )}
            </div>
          </div>

          {/* Right: Formula + Examples */}
          <div className="lg:col-span-2 space-y-5">

            {/* Formula card */}
            <div className="bg-[#0F1B35] text-white rounded-2xl p-5">
              <div className="flex items-center gap-2 mb-4">
                <TrendingUp size={16} className="text-blue-300"/>
                <h3 className="font-bold text-sm">EMI Formula (Full/Flat Interest)</h3>
              </div>
              <p className="font-mono text-xs leading-6 text-blue-100 bg-white/10 rounded-lg p-3 mb-3">
                Interest = P × Rate × (Months ÷ 12)<br/>
                Total = P + Interest<br/>
                EMI = Total ÷ Months
              </p>
              <ul className="text-xs text-blue-200 space-y-1">
                <li><strong className="text-white">P</strong> = Principal (Loan Amount)</li>
                <li><strong className="text-white">Rate</strong> = Annual Rate / 100</li>
                <li><strong className="text-white">Months</strong> = Tenure in Months</li>
                <li><strong className="text-white">Rate = 0:</strong> EMI = P / Months</li>
              </ul>
            </div>

            {/* Quick examples */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
              <h3 className="font-bold text-gray-900 text-sm mb-3">Quick Examples</h3>
              <div className="space-y-2">
                {TEST_CASES.map((ex, i) => {
                  const r = calculateEMI(ex.principal, ex.rate, ex.months);
                  return (
                    <button
                      key={i}
                      onClick={() => loadExample(ex)}
                      className="w-full flex items-center justify-between p-3 rounded-xl border border-gray-100 hover:border-[#1E88E5] hover:bg-blue-50 transition-all text-left group"
                    >
                      <div>
                        <p className="text-xs font-bold text-gray-800 group-hover:text-[#1E88E5]">{ex.desc}</p>
                        <p className="text-[10px] text-gray-400 mt-0.5">{ex.months} months · {ex.rate}% p.a.</p>
                      </div>
                      {r.valid && (
                        <span className="text-xs font-bold text-[#1E88E5] bg-blue-50 group-hover:bg-white px-2.5 py-1 rounded-lg whitespace-nowrap">
                          {fmtINR(r.emi)}/mo
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Note */}
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-xs text-amber-800">
              <strong>Note:</strong> This calculator uses the <strong>Full/Flat Interest</strong> method.
              Interest is calculated on the <em>original loan amount</em> for the entire tenure —
              not reduced month by month. The Finance module uses the same formula — results will always match.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
