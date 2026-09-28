package com.autoconsultancy.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinanceDetailRequest {

    private Boolean underFinance;

    // ── If underFinance = true: core loan parameters ──────────────────────────
    /** Loan principal (required when underFinance=true). */
    @DecimalMin(value = "1000.00", message = "Loan amount must be at least ₹1,000")
    @DecimalMax(value = "10000000.00", message = "Loan amount must not exceed ₹1,00,00,000")
    @Digits(integer = 10, fraction = 2, message = "Loan amount must have at most 2 decimal places")
    private BigDecimal loanAmount;

    /** Annual interest rate in percent, e.g. 12.0 for 12%. */
    @DecimalMin(value = "0.01", message = "Interest rate must be greater than 0")
    @DecimalMax(value = "100.00", message = "Interest rate must not exceed 100%")
    @Digits(integer = 3, fraction = 4, message = "Invalid interest rate format")
    private BigDecimal annualInterestRate;

    /** Tenure in years (LEGACY — prefer tenureMonths). */
    @Min(value = 1, message = "Tenure must be at least 1 year")
    @Max(value = 30, message = "Tenure must not exceed 30 years")
    private Integer tenureYears;

    /** Tenure in months (PRIMARY). */
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 360, message = "Tenure must not exceed 360 months")
    private Integer tenureMonths;

    // ── Calculated fields — backend always recalculates; client values ignored ─
    private BigDecimal emiAmount;
    private Integer numberOfEmis;
    private BigDecimal totalPayable;
    private BigDecimal totalInterest;

    // ── Existing loan tracking ────────────────────────────────────────────────
    @Size(max = 200, message = "Finance company name must not exceed 200 characters")
    private String financeCompany;

    @Size(max = 100, message = "Loan account number must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-/]*$", message = "Loan account number contains invalid characters")
    private String loanAccountNumber;

    private LocalDate loanStartDate;

    @Min(value = 0, message = "Paid EMIs cannot be negative")
    @Max(value = 360, message = "Paid EMIs cannot exceed 360")
    private Integer paidEmis;

    @DecimalMin(value = "0.00", message = "Outstanding loan amount cannot be negative")
    @DecimalMax(value = "10000000.00", message = "Outstanding loan amount exceeds maximum")
    @Digits(integer = 10, fraction = 2, message = "Invalid outstanding amount format")
    private BigDecimal outstandingLoanAmount;

    private LocalDate nextEmiDueDate;

    @Size(max = 50, message = "EMI payment status too long")
    private String emiPaymentStatus;

    @Size(max = 50, message = "Loan closure status too long")
    private String loanClosureStatus;

    private Boolean nocAvailable;

    // ── If underFinance = false ───────────────────────────────────────────────
    private Boolean bikePaid;

    @Size(max = 100, message = "Purchase payment method too long")
    private String purchasePaymentMethod;

    // ── Finance lifecycle status ──────────────────────────────────────────────
    @Size(max = 50, message = "Finance status too long")
    private String financeStatus;
}
