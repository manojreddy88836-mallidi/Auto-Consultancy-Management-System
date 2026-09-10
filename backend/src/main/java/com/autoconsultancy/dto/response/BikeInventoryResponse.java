package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BikeInventoryResponse {
    private Long id;
    private String bikeCode;
    private String registrationNumber;

    // Bike Model info (denormalized for convenience)
    private Long bikeModelId;
    private String modelName;
    private String category;

    // Manufacturer info
    private Long manufacturerId;
    private String manufacturerName;

    // Inventory-specific fields
    private BigDecimal price;
    private Integer year;
    private String color;
    private Integer kmDriven;
    private String fuelType;
    private String conditionType;
    private String description;
    private String saleStatus;        // NOT_FOR_SALE | AVAILABLE | RESERVED | SOLD
    private boolean active;
    private int imageCount;

    /** Primary image URL for thumbnail display */
    private String primaryImageUrl;

    /** All images — populated in detail view */
    private List<BikeInventoryImageResponse> images;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
