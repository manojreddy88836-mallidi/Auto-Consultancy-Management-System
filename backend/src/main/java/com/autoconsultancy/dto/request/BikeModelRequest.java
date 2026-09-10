package com.autoconsultancy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BikeModelRequest {
    @NotNull(message = "Manufacturer ID is required")
    private Long manufacturerId;

    @NotBlank(message = "Model name is required")
    private String modelName;

    private String category;
    private String fuelType;
    private Boolean active = true;

    // ── Sale availability ──────────────────────────────────────────────────
    private Boolean availableForSale = false;
    private String saleStatus;   // NOT_FOR_SALE | AVAILABLE | RESERVED | SOLD
    private BigDecimal price;
}
