package com.autoconsultancy.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * EmiCalculator — Backend single source of truth for EMI calculation.
 *
 * FORMULA: Full / Flat Interest Method
 *
 *   Interest     = Principal × (AnnualRate / 100) × (TenureMonths / 12)
 *   TotalPayable = Principal + Interest
 *   Monthly EMI  = TotalPayable / TenureMonths
 *
 * Interest is always calculated on the ORIGINAL loan amount for the full tenure.
 * Zero-interest case: EMI = Principal / TenureMonths
 */
public final class EmiCalculator {

    private EmiCalculator() { /* utility class */ }

    /**
     * Calculate the monthly EMI using the Full/Flat Interest method.
     * PRIMARY method — accepts tenure in MONTHS directly.
     *
     * @param principal      Loan amount (positive)
     * @param annualRatePct  Annual interest rate in percent (>= 0), e.g. 12.0 for 12%
     * @param tenureMonths   Loan tenure in MONTHS (positive), e.g. 12, 24, 36, 60
     * @return               Monthly EMI rounded to 2 decimal places
     */
    public static BigDecimal monthlyEmi(BigDecimal principal, BigDecimal annualRatePct, int tenureMonths) {
        validateMonths(principal, annualRatePct, tenureMonths);

        int n = tenureMonths; // months used directly — no conversion needed

        // Zero-interest case
        if (annualRatePct.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        }

        // Full / Flat Interest formula
        // Interest = P × (Rate / 100) × (n / 12)
        // TotalPayable = P + Interest
        // EMI = TotalPayable / n
        double P              = principal.doubleValue();
        double rate           = annualRatePct.doubleValue();
        double tenureYears    = (double) n / 12.0;
        double totalInterest  = P * (rate / 100.0) * tenureYears;
        double totalPayable   = P + totalInterest;
        double emi            = totalPayable / n;

        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total payable using Full/Flat Interest.
     * TotalPayable = Principal + Interest
     * Interest = Principal × (AnnualRate/100) × (TenureMonths/12)
     */
    public static BigDecimal totalPayable(BigDecimal principal, BigDecimal annualRatePct, int tenureMonths) {
        if (annualRatePct.compareTo(BigDecimal.ZERO) == 0) {
            return principal.setScale(2, RoundingMode.HALF_UP);
        }
        double P             = principal.doubleValue();
        double rate          = annualRatePct.doubleValue();
        double tenureYears   = (double) tenureMonths / 12.0;
        double totalInterest = P * (rate / 100.0) * tenureYears;
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

    // Validation

    public static void validateMonths(BigDecimal principal, BigDecimal annualRatePct, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Loan amount must be greater than 0.");
        if (annualRatePct == null || annualRatePct.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Interest rate cannot be negative.");
        if (tenureMonths < 1)
            throw new IllegalArgumentException("Tenure must be at least 1 month.");
    }

    /**
     * @deprecated Use monthlyEmi(principal, annualRatePct, tenureMonths) instead.
     *             This overload converts years to months for backward compatibility only.
     */
    @Deprecated
    public static BigDecimal monthlyEmiYears(BigDecimal principal, BigDecimal annualRatePct, int tenureYears) {
        return monthlyEmi(principal, annualRatePct, tenureYears * 12);
    }
}
