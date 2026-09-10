package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class BikeDetailResponse {
    private Long id;
    private Long manufacturerId;
    private String manufacturerName;
    private Long bikeModelId;
    private String modelName;
    private Long variantId;
    private String variantName;
    private Integer manufacturingYear;
    private String registrationNumber;
    private String colour;
    private LocalDate purchaseDate;
}
