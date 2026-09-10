package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationResponse {
    private Long id;
    private String applicationNumber;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String manufacturerName;
    private String modelName;
    private String variantName;
    private Integer manufacturingYear;
    private String registrationNumber;
    private String status;
    private Boolean underFinance;
    private String financeCompany;
    private String assignedWorkerName;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    // ── Finance computed fields (populated from FinanceDetail) ────────────────
    private java.math.BigDecimal loanAmount;
    private java.math.BigDecimal annualInterestRate;
    private Integer tenureYears;    // LEGACY
    private Integer tenureMonths;   // PRIMARY
    private java.math.BigDecimal emiAmount;
    private java.math.BigDecimal totalPayable;
    private java.math.BigDecimal totalInterest;
    private Integer numberOfEmis;
    private Integer paidEmis;
    private Integer remainingEmis;
    private String loanClosureStatus;
    private String financeStatus;
    private java.math.BigDecimal outstandingLoanAmount;

    // ── EMI Overdue tracking ───────────────────────────────────────────────────
    private Long financeDetailId;
    private Integer missedEmiMonths;
    private String overdueStatus;           // ON_TIME / ONE_MONTH / TWO_MONTHS / CRITICAL
    private java.time.LocalDate lastEmiPaidDate;
    private java.time.LocalDate loanStartDate;
    private java.time.LocalDate nextEmiDueDate;

    // ── Bike sale info (populated when bike is from sale inventory) ────────────
    private Long bikeModelId;
    private String bikeSaleStatus;   // AVAILABLE | RESERVED | SOLD | NOT_FOR_SALE
    private String bikeImageUrl;     // Primary image URL for display in admin/worker list
    private String bikeCategory;
    private String bikeFuelType;
    private java.math.BigDecimal bikePrice; // Price of the physical BikeInventory item

    // ── Bike Inventory reference (new — exact physical bike) ─────────────────
    private Long bikeInventoryId;
    private String bikeCode;         // e.g. RE-H350-001
}
