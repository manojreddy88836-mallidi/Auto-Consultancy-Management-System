package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerSummaryDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String city;
    private String state;
}
