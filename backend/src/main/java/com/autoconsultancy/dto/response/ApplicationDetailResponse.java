package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApplicationDetailResponse {
    private Long id;
    private String applicationNumber;
    private String status;
    private String remarks;

    private CustomerSummaryDto customer;
    private BikeDetailResponse bikeDetail;
    private FinanceDetailResponse financeDetail;
    private List<DocumentResponse> documents;
    private List<StatusHistoryResponse> statusHistory;
    private WorkerAssignmentResponse workerAssignment;

    // ── Flat customer fields (mirror of customer.* for easy frontend access) ──
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // ── Bike sale info (populated when bike is from sale inventory) ───────────
    private Long bikeModelId;
    private String bikeSaleStatus;      // AVAILABLE | RESERVED | SOLD | NOT_FOR_SALE
    private String bikeImageUrl;        // Primary image URL
    private String bikeCategory;
    private String bikeFuelType;
    private java.math.BigDecimal bikePrice;

    // ── Flat bike fields (mirror of bikeDetail.* for easy frontend access) ───
    private String manufacturerName;
    private String modelName;
    private String variantName;
    private Integer manufacturingYear;
    private String registrationNumber;

    // ── Bike Inventory reference (exact physical bike) ───────────────────────
    private Long bikeInventoryId;
    private String bikeCode;            // e.g. RE-H350-001

    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}

