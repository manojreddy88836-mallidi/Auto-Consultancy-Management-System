package com.autoconsultancy.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BikeDetailRequest {
    private Long manufacturerId;
    private Long bikeModelId;
    private Long variantId;
    private Integer manufacturingYear;
    private String registrationNumber;
    private String colour;
    private LocalDate purchaseDate;
}
