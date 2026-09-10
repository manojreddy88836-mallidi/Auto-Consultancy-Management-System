package com.autoconsultancy.dto.request;

import lombok.Data;

@Data
public class WorkerAssignmentRequest {
    private Long workerId;
    private String notes;
}
