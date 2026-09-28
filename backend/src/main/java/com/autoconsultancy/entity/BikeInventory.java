package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "bike_inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeInventory {

    public enum SaleStatus { NOT_FOR_SALE, AVAILABLE, RESERVED, SOLD }

    @Id
    private Long id;

    @DBRef
    private BikeModel bikeModel;

    @Indexed(unique = true)
    private String bikeCode;

    private String registrationNumber;
    private BigDecimal price;
    private Integer year;
    private String color;
    private Integer kmDriven;
    private String fuelType;
    private String conditionType;
    private String description;

    @Builder.Default
    private SaleStatus saleStatus = SaleStatus.NOT_FOR_SALE;

    @Builder.Default
    private boolean active = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @DBRef
    @Builder.Default
    private List<BikeInventoryImage> images = new ArrayList<>();
}
