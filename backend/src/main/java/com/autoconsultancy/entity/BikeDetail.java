package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "bike_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    @JsonIgnore
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_model_id")
    private BikeModel bikeModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private BikeVariant variant;

    /** References the exact physical bike inventory item.
     *  Populated when a customer applies from Browse Bikes → Apply Now. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_inventory_id")
    private BikeInventory bikeInventory;

    private Integer manufacturingYear;
    private String registrationNumber;
    private String colour;
    private LocalDate purchaseDate;
}
