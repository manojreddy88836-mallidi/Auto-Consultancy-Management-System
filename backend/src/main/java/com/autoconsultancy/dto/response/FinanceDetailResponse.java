package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class FinanceDetailResponse {
    private Long id;
    private Boolean underFinance;

    // ── Loan details ──────────────────────────────────────────────────────────
    private String financeCompany;
    private String loanAccountNumber;
    private LocalDate loanStartDate;
    private BigDecimal loanAmount;

    // ── EMI calculation inputs ────────────────────────────────────────────────
    private BigDecimal annualInterestRate;  // % p.a., e.g. 12.00
    private Integer tenureYears;           // LEGACY — kept for backward compatibility
    private Integer tenureMonths;          // PRIMARY — tenure in months

    // ── Calculated EMI results ────────────────────────────────────────────────
    private BigDecimal emiAmount;           // monthly EMI
    private Integer numberOfEmis;          // total months
    private Integer paidEmis;
    private Integer remainingEmis;
    private BigDecimal totalPayable;       // emi × months
    private BigDecimal totalInterest;      // totalPayable − principal

    // ── Loan tracking ─────────────────────────────────────────────────────────
    private BigDecimal outstandingLoanAmount;
    private LocalDate nextEmiDueDate;
    private String emiPaymentStatus;
    private String loanClosureStatus;
    private Boolean nocAvailable;

    // ── No-finance case ───────────────────────────────────────────────────────
    private Boolean bikePaid;
    private String purchasePaymentMethod;
    private String financeStatus;
}
