package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bike_models")
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manufacturer_id", nullable = false)
    @JsonIgnore
    private Manufacturer manufacturer;

    @Column(nullable = false)
    private String modelName;

    private String category;
    private String fuelType;

    @Builder.Default
    private boolean active = true;

    // ── Sale Availability ──────────────────────────────────────────────────
    @Builder.Default
    @Column(name = "available_for_sale")
    private Boolean availableForSale = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status")
    private SaleStatus saleStatus = SaleStatus.NOT_FOR_SALE;

    private BigDecimal price;  // optional price field

    // ── Timestamps ─────────────────────────────────────────────────────────
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Relations ──────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "bikeModel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BikeVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "bikeModel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ManufacturingYear> manufacturingYears = new ArrayList<>();

    @OneToMany(mappedBy = "bikeModel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BikeImage> images = new ArrayList<>();
}
