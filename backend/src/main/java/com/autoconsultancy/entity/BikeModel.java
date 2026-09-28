package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "bike_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeModel {

    public enum SaleStatus {
        NOT_FOR_SALE, AVAILABLE, RESERVED, SOLD
    }

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private Manufacturer manufacturer;

    private String modelName;
    private String category;
    private String fuelType;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Boolean availableForSale = false;

    @Builder.Default
    private SaleStatus saleStatus = SaleStatus.NOT_FOR_SALE;

    private BigDecimal price;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @DBRef
    @Builder.Default
    private List<BikeVariant> variants = new ArrayList<>();

    @DBRef
    @Builder.Default
    private List<ManufacturingYear> manufacturingYears = new ArrayList<>();

    @DBRef
    @Builder.Default
    private List<BikeImage> images = new ArrayList<>();
}
