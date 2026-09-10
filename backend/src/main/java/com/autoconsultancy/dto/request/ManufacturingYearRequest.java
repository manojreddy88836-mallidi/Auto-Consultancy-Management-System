package com.autoconsultancy.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManufacturingYearRequest {
    private Long bikeModelId;
    @NotNull(message = "Year is required")
    private Integer year;
    private Boolean active = true;
}
