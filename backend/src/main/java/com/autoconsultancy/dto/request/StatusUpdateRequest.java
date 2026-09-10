package com.autoconsultancy.dto.request;

import lombok.Data;

@Data
public class StatusUpdateRequest {
    private String status;
    private String remarks;
}
