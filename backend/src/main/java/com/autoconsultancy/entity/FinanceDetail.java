package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "finance_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceDetail {

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private Application application;

    private boolean underFinance;

    // ── Core loan details ─────────────────────────────────────────────────────
    private String financeCompany;
    private String loanAccountNumber;
    private LocalDate loanStartDate;
    private BigDecimal loanAmount;

    // ── EMI calculation inputs ────────────────────────────────────────────────
    private BigDecimal annualInterestRate;   // monthly interest rate %
    private Integer tenureYears;            // LEGACY
    private Integer tenureMonths;           // PRIMARY

    // ── Calculated EMI fields ─────────────────────────────────────────────────
    private BigDecimal emiAmount;
    private Integer numberOfEmis;
    private Integer paidEmis;
    private Integer remainingEmis;
    private BigDecimal totalPayable;
    private BigDecimal totalInterest;

    // ── Loan tracking ─────────────────────────────────────────────────────────
    private BigDecimal outstandingLoanAmount;
    private LocalDate nextEmiDueDate;
    private String emiPaymentStatus;
    private String loanClosureStatus;
    private boolean nocAvailable;

    // ── EMI Overdue Tracking ──────────────────────────────────────────────────
    private Integer missedEmiMonths;
    private String overdueStatus;
    private LocalDate lastEmiPaidDate;

    // ── No-finance case ───────────────────────────────────────────────────────
    private boolean bikePaid;
    private String purchasePaymentMethod;
    private String financeStatus;
}
