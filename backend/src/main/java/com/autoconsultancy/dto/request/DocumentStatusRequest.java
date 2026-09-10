package com.autoconsultancy.dto.request;

import lombok.Data;

@Data
public class DocumentStatusRequest {
    private String status;
    private String remarks;
}
