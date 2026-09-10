package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bike_offers")
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The exact physical bike being negotiated */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_inventory_id", nullable = false)
    private BikeInventory bikeInventory;

    /** Customer who made the offer */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Snapshot of the bike's listed price at offer time — never changes */
    @Column(name = "listed_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal listedPrice;

    /** Customer's offered price */
    @Column(name = "offered_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal offeredPrice;

    /** Counter price set by admin/worker (null unless status=COUNTER_OFFER) */
    @Column(name = "counter_offer_price", precision = 12, scale = 2)
    private BigDecimal counterOfferPrice;

    /**
     * The final agreed / negotiated price — recorded when deal is closed.
     * Set to offeredPrice on ACCEPTED, or counterOfferPrice on COUNTER_ACCEPTED.
     * The bike's listed price (bike_inventory.price) is NEVER modified.
     */
    @Column(name = "agreed_price", precision = 12, scale = 2)
    private BigDecimal agreedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OfferStatus status = OfferStatus.PENDING;

    /** Optional message from the customer */
    @Column(name = "customer_message", columnDefinition = "TEXT")
    private String customerMessage;

    /** Response note from admin/worker */
    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    /** Who responded (admin or worker user) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_user_id")
    private User respondedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
