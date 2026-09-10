package com.autoconsultancy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManufacturerRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String logoUrl;
    private String country;
    private boolean active = true;
}
