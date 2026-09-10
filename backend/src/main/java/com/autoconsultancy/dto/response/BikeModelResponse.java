package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BikeModelResponse {
    private Long id;
    private Long manufacturerId;
    private String manufacturerName;
    private String modelName;
    private String category;
    private String fuelType;

    /**
     * Inventory-derived active status.
     * true  = at least one AVAILABLE bike exists in the Bikes/Inventory module for this model.
     * false = no AVAILABLE bikes (zero inventory, all sold, all reserved, etc.).
     * This field is READ-ONLY and is NOT controlled manually.
     */
    private Boolean active;

    /** Number of AVAILABLE bikes in inventory for this model. */
    private Long availableInventoryCount;

    // ── Legacy sale availability (kept for backward compatibility, deprecated) ─
    private Boolean availableForSale;
    private String saleStatus;          // NOT_FOR_SALE | AVAILABLE | RESERVED | SOLD
    private BigDecimal price;

    // ── Images ─────────────────────────────────────────────────────────────
    private List<BikeImageResponse> images;
    private String primaryImageUrl;     // convenience field for listing pages

    // ── Variants / Years ───────────────────────────────────────────────────
    private List<BikeVariantResponse> variants;
    private List<Integer> years;

    private LocalDateTime createdAt;
}
