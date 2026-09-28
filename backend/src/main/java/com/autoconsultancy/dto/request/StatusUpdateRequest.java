package com.autoconsultancy.dto.request;

import lombok.Data;

@Data
public class StatusUpdateRequest {
    private String status;
    private String newStatus;
    private String remarks;

    public String getStatus() {
        return status != null ? status : newStatus;
    }
}
