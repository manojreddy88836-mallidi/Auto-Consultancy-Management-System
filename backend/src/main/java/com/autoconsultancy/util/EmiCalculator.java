package com.autoconsultancy.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * EmiCalculator — Backend single source of truth for EMI calculation.
 *
 * FORMULA: Full / Flat Interest Method  (Monthly Rate)
 *
 *   monthlyRate  = MonthlyRatePct / 100           (e.g. 2% → 0.02)
 *   Interest     = Principal × monthlyRate × TenureMonths
 *   TotalPayable = Principal + Interest
 *   Monthly EMI  = TotalPayable / TenureMonths
 *
 * The stored field "annualInterestRate" now holds the MONTHLY rate (% per month).
 * The DB column name is kept unchanged for backward compatibility.
 * Interest is always on the ORIGINAL principal for the COMPLETE tenure.
 * Zero-interest case: EMI = Principal / TenureMonths
 */
public final class EmiCalculator {

    private EmiCalculator() { /* utility class */ }

    /**
     * Calculate the monthly EMI using the Full/Flat Interest method (Monthly Rate).
     * PRIMARY method — accepts tenure in MONTHS directly.
     *
     * @param principal       Loan amount (positive)
     * @param monthlyRatePct  Monthly interest rate in percent (>= 0), e.g. 2.0 for 2%/month
     * @param tenureMonths    Loan tenure in MONTHS (positive), e.g. 12, 24, 36, 60
     * @return                Monthly EMI rounded to 2 decimal places
     */
    public static BigDecimal monthlyEmi(BigDecimal principal, BigDecimal monthlyRatePct, int tenureMonths) {
        validate(principal, monthlyRatePct, tenureMonths);

        int n = tenureMonths; // months used directly

        // Zero-interest case
        if (monthlyRatePct.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        }

        // Full / Flat Interest formula (Monthly Rate)
        // Interest = P × (rate / 100) × n   ← rate is monthly %, n is months
        // TotalPayable = P + Interest
        // EMI = TotalPayable / n
        double P             = principal.doubleValue();
        double rate          = monthlyRatePct.doubleValue(); // monthly %
        double rateDecimal   = rate / 100.0;                 // e.g. 0.02
        double totalInterest = P * rateDecimal * n;          // flat, monthly rate
        double totalPayable  = P + totalInterest;
        double emi           = totalPayable / n;

        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total payable using Full/Flat Interest (Monthly Rate).
     * TotalPayable = Principal + (Principal × monthlyRate × tenureMonths)
     *
     * @param principal       Loan amount
     * @param monthlyRatePct  Monthly interest rate in percent (% per month)
     * @param tenureMonths    Tenure in months
     */
    public static BigDecimal totalPayable(BigDecimal principal, BigDecimal monthlyRatePct, int tenureMonths) {
        if (monthlyRatePct.compareTo(BigDecimal.ZERO) == 0) {
            return principal.setScale(2, RoundingMode.HALF_UP);
        }
        double P             = principal.doubleValue();
        double rate          = monthlyRatePct.doubleValue(); // monthly %
        double rateDecimal   = rate / 100.0;
        double totalInterest = P * rateDecimal * tenureMonths;
        double total         = P + totalInterest;
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total interest (TotalPayable - Principal).
     */
    public static BigDecimal totalInterest(BigDecimal totalPayable, BigDecimal principal) {
        return totalPayable.subtract(principal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Remaining EMIs = totalEmis - paidEmis (clamped to >= 0).
     */
    public static int remainingEmis(int totalEmis, int paidEmis) {
        return Math.max(0, totalEmis - paidEmis);
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    public static void validate(BigDecimal principal, BigDecimal monthlyRatePct, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Loan amount must be greater than 0.");
        if (monthlyRatePct == null || monthlyRatePct.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Interest rate cannot be negative.");
        if (tenureMonths < 1)
            throw new IllegalArgumentException("Tenure must be at least 1 month.");
    }

    /** Keep old name as alias for compatibility. */
    public static void validateMonths(BigDecimal principal, BigDecimal monthlyRatePct, int tenureMonths) {
        validate(principal, monthlyRatePct, tenureMonths);
    }

    /**
     * @deprecated Use monthlyEmi(principal, monthlyRatePct, tenureMonths) instead.
     */
    @Deprecated
    public static BigDecimal monthlyEmiYears(BigDecimal principal, BigDecimal monthlyRatePct, int tenureYears) {
        return monthlyEmi(principal, monthlyRatePct, tenureYears * 12);
    }
}
