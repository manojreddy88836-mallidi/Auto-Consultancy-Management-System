package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "bike_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeDetail {

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private Application application;

    @DBRef
    private Manufacturer manufacturer;

    @DBRef
    private BikeModel bikeModel;

    @DBRef
    private BikeVariant variant;

    @DBRef
    private BikeInventory bikeInventory;

    private Integer manufacturingYear;
    private String registrationNumber;
    private String colour;
    private LocalDate purchaseDate;
}
