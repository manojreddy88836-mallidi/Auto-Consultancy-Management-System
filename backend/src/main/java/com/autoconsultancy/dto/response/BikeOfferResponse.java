package com.autoconsultancy.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BikeOfferResponse {

    private Long id;

    // ── Bike info (read-only snapshot) ────────────────────────────────────
    private Long   bikeInventoryId;
    private String bikeCode;
    private String manufacturerName;
    private String modelName;
    private String variantName;
    private Integer manufacturingYear;
    private String  registrationNumber;
    private String  colour;
    private String  fuelType;
    private String  conditionType;
    private String  bikeImageUrl;       // primary image URL

    // ── Prices ────────────────────────────────────────────────────────────
    /** Bike's listed price at the time the offer was made (snapshot) */
    private BigDecimal listedPrice;

    /** Customer's offered price */
    private BigDecimal offeredPrice;

    /** Counter offer from admin/worker (null if no counter) */
    private BigDecimal counterOfferPrice;

    /**
     * Final agreed / negotiated price — set when offer is ACCEPTED or COUNTER_ACCEPTED.
     * Always distinct from listedPrice (the inventory price which is never modified).
     */
    private BigDecimal agreedPrice;

    // ── Offer status & negotiation ────────────────────────────────────────
    private String status;

    private String customerMessage;
    private String adminResponse;

    // ── Customer info ─────────────────────────────────────────────────────
    private Long   customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // ── Responder info ────────────────────────────────────────────────────
    private String respondedByName;

    // ── Timestamps ────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
