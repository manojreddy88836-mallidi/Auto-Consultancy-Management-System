/**
 * EMI CALCULATOR UTILITY - Single source of truth for all EMI calculations
 * Used by: EMI Calculator page (public), Finance form (customer),
 *          Finance Records (admin), and backend validation.
 *
 * FORMULA: Full / Flat Interest Method  (Monthly Rate)
 *
 *   monthlyRate  = MonthlyRatePct / 100          (e.g. 2% → 0.02)
 *   Interest     = Principal × monthlyRate × TenureMonths
 *   TotalPayable = Principal + Interest
 *   Monthly EMI  = TotalPayable / TenureMonths
 *
 * The entered rate is treated as a MONTHLY percentage (% per month).
 * Example: 2 means 2% per month, NOT 2% per year.
 *
 * ZERO-INTEREST CASE: EMI = Principal / TenureMonths
 */

/**
 * Calculate EMI using the Full/Flat Interest formula (Monthly Rate).
 *
 * @param {number} principal      - Loan amount (> 0)
 * @param {number} monthlyRate    - Monthly interest rate (% per month) e.g. 2 for 2%/month
 * @param {number} tenureMonths   - Loan tenure in MONTHS e.g. 12, 24, 36, 60
 * @returns {{ emi, totalPayable, totalInterest, totalMonths, valid, error }}
 */
export function calculateEMI(principal, monthlyRate, tenureMonths) {
  // Input coercion
  const P = Number(principal);
  const R = Number(monthlyRate);   // % per month, e.g. 2 = 2% per month
  const n = Math.round(Number(tenureMonths)); // total months — must be integer

  // Validation
  if (!isFinite(P) || P <= 0)
    return { valid: false, error: 'Loan amount must be greater than 0.' };
  if (!isFinite(R) || R < 0)
    return { valid: false, error: 'Interest rate cannot be negative.' };
  if (!isFinite(n) || n < 1)
    return { valid: false, error: 'Tenure must be at least 1 month.' };

  // Full / Flat Interest (Monthly Rate) calculation
  // Interest is always on the ORIGINAL principal for the COMPLETE tenure
  // Rate is monthly %, so no /12 conversion needed
  const rateDecimal   = R / 100;          // e.g. 2% → 0.02
  const totalInterest = P * rateDecimal * n; // P × r × n  (flat, not reducing)
  const totalPayable  = P + totalInterest;
  const emi           = totalPayable / n;

  return {
    valid:         true,
    emi:           round2(emi),
    totalPayable:  round2(totalPayable),
    totalInterest: round2(totalInterest),
    totalMonths:   n,
    monthlyRate:   R, // monthly rate in %
  };
}

/**
 * Round a number to 2 decimal places (standard currency rounding).
 */
export function round2(value) {
  return Math.round(value * 100) / 100;
}

/**
 * Format a number as Indian currency string: Rs.11,23,456.78
 */
export function fmtINR(value) {
  if (value === null || value === undefined || isNaN(value)) return '-';
  return 'Rs.' + Number(value).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

/**
 * Convenience: recalculate the outstanding balance based on remaining EMIs.
 *  outstandingApprox = remainingEmis x emi
 */
export function estimateOutstanding(emi, remainingEmis) {
  if (!emi || !remainingEmis) return 0;
  return round2(Number(emi) * Number(remainingEmis));
}
