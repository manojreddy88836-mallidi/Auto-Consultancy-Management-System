package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BikeVariantResponse {
    private Long id;
    private String variantName;
    private Integer engineCC;
    private Boolean active;
}
