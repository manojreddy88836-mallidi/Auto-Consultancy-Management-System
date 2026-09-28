package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "bike_offers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BikeOffer {

    public enum OfferStatus {
        PENDING,        // Customer submitted, awaiting response
        ACCEPTED,       // Admin/worker accepted customer's offer
        REJECTED,       // Admin/worker rejected
        COUNTER_OFFER,  // Admin/worker sent a counter price
        COUNTER_ACCEPTED, // Customer accepted counter offer
        COUNTER_REJECTED, // Customer rejected counter offer
        EXPIRED,        // Offer expired without action
        WITHDRAWN       // Customer cancelled before response
    }

    @Id
    private Long id;

    @DBRef
    private BikeInventory bikeInventory;

    @DBRef
    private Customer customer;

    private BigDecimal listedPrice;
    private BigDecimal offeredPrice;
    private BigDecimal counterOfferPrice;
    private BigDecimal agreedPrice;

    @Builder.Default
    private OfferStatus status = OfferStatus.PENDING;

    private String customerMessage;
    private String adminResponse;

    @DBRef
    private User respondedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
