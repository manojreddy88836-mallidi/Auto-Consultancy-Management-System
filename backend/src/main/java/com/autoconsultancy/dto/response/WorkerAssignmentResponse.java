package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class WorkerAssignmentResponse {
    private Long id;
    private Long workerId;
    private String workerName;
    private String workerEmail;
    private String notes;
    private LocalDateTime assignedAt;
}
