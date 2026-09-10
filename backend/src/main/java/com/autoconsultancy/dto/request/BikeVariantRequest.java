package com.autoconsultancy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BikeVariantRequest {
    private Long bikeModelId;
    @NotBlank(message = "Variant name is required")
    private String variantName;
    private Integer engineCC;
    private Boolean active = true;
}
