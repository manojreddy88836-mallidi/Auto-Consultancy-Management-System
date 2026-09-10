package com.autoconsultancy.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinanceDetailRequest {
    private Boolean underFinance;

    // ── If underFinance = true: NEW EMI fields (auto-calculated from these 3) ──
    /** Loan principal (required when underFinance=true) */
    private BigDecimal loanAmount;
    /** Annual interest rate in percent, e.g. 12.0 for 12% (required) */
    private BigDecimal annualInterestRate;
    /** Tenure in years, e.g. 3  (LEGACY — use tenureMonths instead) */
    private Integer tenureYears;

    /** Tenure in months, e.g. 36  (PRIMARY — used to derive numberOfEmis) */
    private Integer tenureMonths;

    // ── Calculated & stored by backend (client may send for display; backend re-validates) ──
    private BigDecimal emiAmount;           // calculated: monthlyEmi
    private Integer numberOfEmis;          // calculated: tenureYears × 12
    private BigDecimal totalPayable;       // calculated: emi × numberOfEmis
    private BigDecimal totalInterest;      // calculated: totalPayable − loanAmount

    // ── Existing loan tracking fields (optional) ──────────────────────────────
    private String  financeCompany;
    private String  loanAccountNumber;
    private LocalDate loanStartDate;
    private Integer paidEmis;
    private BigDecimal outstandingLoanAmount;
    private LocalDate nextEmiDueDate;
    private String  emiPaymentStatus;
    private String  loanClosureStatus;
    private Boolean nocAvailable;

    // ── If underFinance = false ───────────────────────────────────────────────
    private Boolean bikePaid;
    private String  purchasePaymentMethod;

    // ── Finance lifecycle status (ACTIVE, COMPLETED, CLOSED, NOC_PENDING, NO_FINANCE) ──
    private String  financeStatus;
}
