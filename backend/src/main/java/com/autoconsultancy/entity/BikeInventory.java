package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bike_inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BikeInventory {

    public enum SaleStatus { NOT_FOR_SALE, AVAILABLE, RESERVED, SOLD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bike_model_id", nullable = false)
    private BikeModel bikeModel;

    /** Human-readable inventory code, e.g. RE-H350-001 */
    @Column(name = "bike_code", unique = true, length = 100)
    private String bikeCode;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    private Integer year;

    @Column(length = 100)
    private String color;

    @Column(name = "km_driven")
    private Integer kmDriven;

    /** Petrol / Electric / CNG etc. */
    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    /** Excellent / Good / Fair / Poor */
    @Column(name = "condition_type", length = 50)
    private String conditionType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", length = 20, nullable = false)
    private SaleStatus saleStatus = SaleStatus.NOT_FOR_SALE;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Images associated with this inventory bike */
    @OneToMany(mappedBy = "bikeInventory", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BikeInventoryImage> images = new ArrayList<>();
}
