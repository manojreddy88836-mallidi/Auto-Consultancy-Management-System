package com.autoconsultancy.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BikeInventoryRequest {
    private Long bikeModelId;          // required — links to catalog
    private String bikeCode;           // optional unique inventory code
    private String registrationNumber;
    private BigDecimal price;
    private Integer year;
    private String color;
    private Integer kmDriven;
    private String fuelType;
    private String conditionType;      // Excellent / Good / Fair / Poor
    private String description;
}
