/**
 * EMI CALCULATOR UTILITY - Single source of truth for all EMI calculations
 * Used by: EMI Calculator page (public), Finance form (customer),
 *          Finance Records (admin), and backend validation.
 *
 * FORMULA: Full / Flat Interest Method
 *
 *   Interest     = Principal × (AnnualRate / 100) × (TenureMonths / 12)
 *   TotalPayable = Principal + Interest
 *   Monthly EMI  = TotalPayable / TenureMonths
 *
 * ZERO-INTEREST CASE: EMI = Principal / TenureMonths
 */

/**
 * Calculate EMI using the Full/Flat Interest formula.
 *
 * @param {number} principal      - Loan amount (> 0)
 * @param {number} annualRate     - Annual interest rate (%) e.g. 2 for 2%
 * @param {number} tenureMonths   - Loan tenure in MONTHS   e.g. 12, 24, 36, 60
 * @returns {{ emi, totalPayable, totalInterest, totalMonths, valid, error }}
 */
export function calculateEMI(principal, annualRate, tenureMonths) {
  // Input coercion
  const P = Number(principal);
  const R = Number(annualRate);
  const n = Math.round(Number(tenureMonths)); // total months — must be integer

  // Validation
  if (!isFinite(P) || P <= 0)
    return { valid: false, error: 'Loan amount must be greater than 0.' };
  if (!isFinite(R) || R < 0)
    return { valid: false, error: 'Interest rate cannot be negative.' };
  if (!isFinite(n) || n < 1)
    return { valid: false, error: 'Tenure must be at least 1 month.' };

  // Full / Flat Interest calculation
  // Interest is always on the ORIGINAL principal for the full tenure
  const tenureYears   = n / 12;
  const totalInterest = P * (R / 100) * tenureYears;  // flat — not reducing
  const totalPayable  = P + totalInterest;
  const emi           = totalPayable / n;

  return {
    valid:         true,
    emi:           round2(emi),
    totalPayable:  round2(totalPayable),
    totalInterest: round2(totalInterest),
    totalMonths:   n,
    monthlyRate:   0, // not applicable for flat interest; kept for API compat
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
