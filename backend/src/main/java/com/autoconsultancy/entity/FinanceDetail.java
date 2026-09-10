package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "finance_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    @JsonIgnore
    private Application application;

    private boolean underFinance;

    // ── Core loan details ─────────────────────────────────────────────────────
    private String financeCompany;
    private String loanAccountNumber;
    private LocalDate loanStartDate;
    private BigDecimal loanAmount;

    // ── NEW: EMI calculation inputs (stored for audit / recalculation) ────────
    @Column(name = "annual_interest_rate", precision = 5, scale = 2)
    private BigDecimal annualInterestRate;   // e.g. 12.00 means 12% p.a.

    @Column(name = "tenure_years")
    private Integer tenureYears;            // LEGACY: kept for backward compatibility

    @Column(name = "tenure_months")
    private Integer tenureMonths;           // NEW: tenure in months (primary field)

    // ── Calculated EMI fields (stored for display / reporting) ────────────────
    private BigDecimal emiAmount;           // monthly EMI (calculated)
    private Integer numberOfEmis;          // tenureYears × 12
    private Integer paidEmis;
    private Integer remainingEmis;

    @Column(name = "total_payable", precision = 12, scale = 2)
    private BigDecimal totalPayable;        // emiAmount × numberOfEmis

    @Column(name = "total_interest", precision = 12, scale = 2)
    private BigDecimal totalInterest;       // totalPayable − loanAmount

    // ── Loan tracking ─────────────────────────────────────────────────────────
    private BigDecimal outstandingLoanAmount;
    private LocalDate nextEmiDueDate;
    private String emiPaymentStatus;
    private String loanClosureStatus;
    private boolean nocAvailable;

    // ── EMI Overdue Tracking (computed by EmiOverdueService) ─────────────────
    @Column(name = "missed_emi_months")
    private Integer missedEmiMonths;        // computed: how many installments are overdue

    @Column(name = "overdue_status")
    private String overdueStatus;           // ON_TIME / ONE_MONTH / TWO_MONTHS / CRITICAL

    @Column(name = "last_emi_paid_date")
    private java.time.LocalDate lastEmiPaidDate;  // date of most recent recorded payment

    // ── No-finance case ───────────────────────────────────────────────────────
    private boolean bikePaid;
    private String purchasePaymentMethod;
    private String financeStatus;
}
